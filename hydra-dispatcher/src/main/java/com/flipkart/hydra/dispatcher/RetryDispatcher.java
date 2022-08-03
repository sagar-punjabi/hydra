/*
 * Copyright 2015 Flipkart Internet, pvt ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flipkart.hydra.dispatcher;

import com.fasterxml.uuid.Generators;
import com.flipkart.hydra.composer.Composer;
import com.flipkart.hydra.composer.DefaultComposer;
import com.flipkart.hydra.composer.exception.ComposerEvaluationException;
import com.flipkart.hydra.composer.exception.ComposerInstantiationException;
import com.flipkart.hydra.dispatcher.dtos.OssCreateDocumentRequest;
import com.flipkart.hydra.dispatcher.exception.DispatchFailedException;
import com.flipkart.hydra.dispatcher.sal.OssSearchClient;
import com.flipkart.hydra.task.DefaultMultiTask;
import com.flipkart.hydra.task.Task;
import com.flipkart.hydra.task.exception.BadCallableException;
import com.flipkart.poseidon.core.PoseidonRequest;
import com.flipkart.poseidon.exception.NonRetryableDataSourceException;
import com.flipkart.poseidon.exception.RetryableDataSourceException;

import java.util.*;
import java.util.concurrent.*;

import com.flipkart.poseidon.internal.APITask;
import com.google.common.collect.ImmutableMap;
import flipkart.lego.api.entities.Request;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

public class RetryDispatcher implements Dispatcher {

    private static final String OSS_CLIENT_URL_STAGE = "http://10.24.4.84";
    private static final String CLIENT_ID = "WH";
    private static final String TENANT_ID = "FKI";
    private static final String DOCUMENT_NAME = "poseidon_retry_requests";
    private static final String DOCUMENT_TYPE = "default";
    private static final Long DOCUMENT_VERSION = 1L;

    private final ExecutorService executor;
    private final ExecutorCompletionService<Object> completionService;
    private final Request request;


    public RetryDispatcher() {
        this(Executors.newCachedThreadPool());
    }

    public RetryDispatcher(ExecutorService executor) {
        this.executor = executor;
        completionService = new ExecutorCompletionService<>(executor);
        this.request = null;
    }

    public RetryDispatcher(ExecutorService executor, Request request) {
        this.executor = executor;
        completionService = new ExecutorCompletionService<>(executor);
        this.request = request;
    }

    @Override
    public Object execute(Map<String, Object> params, Map<String, Task> tasks, Object context) throws DispatchFailedException, ComposerEvaluationException {
        return execute(params, tasks, context, false);
    }

    @Override
    public Object execute(Map<String, Object> params, Map<String, Task> tasks, Object context, boolean isAlreadyParsed) throws DispatchFailedException, ComposerEvaluationException {
        try {
            DefaultComposer defaultComposer = new DefaultComposer(context, isAlreadyParsed);
            return execute(params, tasks, defaultComposer);
        } catch (ComposerInstantiationException e) {
            throw new DispatchFailedException("Unable to create composer.", e);
        }
    }

    @Override
    public Object execute(Map<String, Object> params, Map<String, Task> tasks, Composer composer) throws DispatchFailedException, ComposerEvaluationException {
        Map<String, Object> responses = dispatchAndCollect(params, tasks, false);

        List<String> dependencies = composer.getDependencies();
        Map<String, Object> collectedDependencies = collectDependencies(responses, dependencies);
        return composer.compose(collectedDependencies);
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }

    private Map<String, Object> dispatchAndCollect(Map<String, Object> params, Map<String, Task> tasks, boolean isCompensating) throws DispatchFailedException, ComposerEvaluationException {
        Map<String, Object> responses = new HashMap<>();
        List<String> dispatched = new ArrayList<>();
        List<String> processed = new ArrayList<>();
        String currentTask = "";
        Map<Future<Object>, String> futures = new HashMap<>();

        responses.putAll(params);

        int remaining = tasks.size();
        while (remaining > 0) {
            for (String key : tasks.keySet()) {
                Task task = tasks.get(key);
                if (!responses.containsKey(key) && !dispatched.contains(key)) {
                    List<String> dependencies = task.getDependencies();
                    Map<String, Object> collectedDependencies = collectDependencies(responses, dependencies);
                    if (collectedDependencies.size() == dependencies.size()) {
                        Future<Object> future = dispatchTask(task, collectedDependencies);
                        dispatched.add(key);
                        futures.put(future, key);

                    }
                }
            }

            if (dispatched.isEmpty()) {
                throw new DispatchFailedException("No possible resolution of dependencies found.");
            }

            try {
                Future future = completionService.take();
                String key = futures.get(future);
                currentTask = key;
                responses.put(key, future.get());
                dispatched.remove(key);
                remaining--;
                processed.add(key);
                System.out.println(processed);
            } catch (InterruptedException | ExecutionException e) {
                if (!isCompensating) {
                    System.out.println("currentTask:" + currentTask);
                    System.out.println("processed:" + processed);
                    try {
                        if (e.getCause() instanceof RetryableDataSourceException) {
                            System.out.println("Retryable exception");
                            DefaultMultiTask dmt = (DefaultMultiTask) tasks.get(currentTask);
                            System.out.println("isModifying:" + dmt.isModifying());
                            if (!dmt.isModifying()) {
                                OssSearchClient ossSearchClient = new OssSearchClient(new RestTemplate(), OSS_CLIENT_URL_STAGE);
                                ossSearchClient.createOSSDocument(buildRequestObject(currentTask, processed, dispatched, remaining, e.getCause(), request), CLIENT_ID, TENANT_ID);
                            }
                        } else if (e.getCause() instanceof NonRetryableDataSourceException)
                            System.out.println("NonRetryable exception");

                    } catch (Exception e1) {
                        System.out.println("No interface implemented by exception");
                        throw e1;
                    }
                    Map<String, Task> compensatingTasks = new HashMap<>();
                    APITask task = (APITask) tasks.get(currentTask);
                    APITask compensatingTask = task.getCompensatingTask();
                    if (compensatingTask != null)
                        compensatingTasks.put(compensatingTask.getName(), compensatingTask);
                    System.out.println("isModifying:" + task.isModifying());
                    for (String processedTask : processed) {
                        task = (APITask) tasks.get(processedTask);
                        compensatingTask = task.getCompensatingTask();
                        if (compensatingTask != null)
                            compensatingTasks.put(compensatingTask.getName(), compensatingTask);
                    }
                    dispatchAndCollect(params, compensatingTasks, true);
                }
                throw new DispatchFailedException("Unable to fetch all required data", e);
            }
        }

        return responses;
    }

    private OssCreateDocumentRequest buildRequestObject(String currentTask, List<String> processed, List<String> dispatched, int remaining, Throwable cause, Request request) {
        Map<String, Object> fieldPairMap = new HashMap<>();
        PoseidonRequest poseidonRequest = (PoseidonRequest) request;
        if (poseidonRequest.getHttpRequest().isPresent()) {
            HttpServletRequest httpRequest = poseidonRequest.getHttpRequest().get();
            Map<String, String[]> attributes = new HashMap<>(httpRequest.getParameterMap());
            attributes.remove("timerContext");
            PoseidonRequest newPoseidonRequest = new PoseidonRequest(httpRequest.getPathInfo(),
                    extractCookies(httpRequest),
                    extractHeaders(httpRequest),
                    attributes
                    );
            fieldPairMap.put("request", newPoseidonRequest);
        }
        fieldPairMap.put("currentTask", currentTask);
        fieldPairMap.put("dispatched", dispatched);
        fieldPairMap.put("processed", processed);
        fieldPairMap.put("remaining", remaining);
        fieldPairMap.put("exception", cause);
        return new OssCreateDocumentRequest(DOCUMENT_NAME, DOCUMENT_TYPE,
                Generators.timeBasedGenerator().toString(), fieldPairMap, System.currentTimeMillis(),
                System.currentTimeMillis(), DOCUMENT_VERSION);

    }

    private Map<String, Object> collectDependencies(Map<String, Object> responses, List<String> dependencies) {
        Map<String, Object> collectedDependencies = new HashMap<>();
        for (String dependency : dependencies) {
            if (responses.containsKey(dependency)) {
                collectedDependencies.put(dependency, responses.get(dependency));
            }
        }

        return collectedDependencies;
    }

    private Future<Object> dispatchTask(Task task, Map<String, Object> responses) throws DispatchFailedException {
        try {
            Callable<Object> callable = task.getCallable(responses);
            return completionService.submit(callable);
        } catch (BadCallableException e) {
            throw new DispatchFailedException("Failed to dispatch task", e);
        }
    }
    private ImmutableMap<String, Cookie> extractCookies(HttpServletRequest httpServletRequest) {
        Map<String, Cookie> cookies = new HashMap<>();
        Cookie[] receivedCookies = httpServletRequest.getCookies();
        if(receivedCookies != null) {
            for (Cookie cookie : receivedCookies) {
                cookies.put(cookie.getName().toLowerCase(), cookie);
            }
        }

        return ImmutableMap.copyOf(cookies);
    }
    private ImmutableMap<String, String> extractHeaders(HttpServletRequest httpServletRequest) {
        Map<String, String> headers = new HashMap<>();
        Enumeration headerNames = httpServletRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = (String) headerNames.nextElement();
            if (httpServletRequest.getHeader(key) != null) {
                headers.put(key.toLowerCase(), httpServletRequest.getHeader(key));
            }
        }

        return ImmutableMap.copyOf(headers);
    }
}
