package com.flipkart.hydra.dispatcher;

import com.flipkart.hydra.task.Task;
import com.flipkart.hydra.composer.Composer;
import com.flipkart.hydra.dispatcher.exception.DispatchFailedException;
import com.flipkart.hydra.expression.exception.ExpressionEvaluationException;

import java.util.Map;

public interface Dispatcher {

    public Object execute(Map<String, Object> params, Map<String, Task> tasks, Composer composer) throws DispatchFailedException, ExpressionEvaluationException;
}
