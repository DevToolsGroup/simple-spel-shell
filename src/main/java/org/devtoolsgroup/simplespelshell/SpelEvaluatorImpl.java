/*
MIT License

Copyright (c) 2026-present DevToolsGroup (https://github.com/DevToolsGroup)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package org.devtoolsgroup.simplespelshell;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.expression.OperatorOverloader;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpelEvaluatorImpl implements SpelEvaluator {
    private final Map<String, Object> variables = new ConcurrentHashMap<>();
    private StandardEvaluationContext spelCtx = new StandardEvaluationContext();
    private final SpelExpressionParser parser = new SpelExpressionParser();

    @Override
    public Object evaluate(Object rootObject, String expression) {
        return parser.parseExpression(expression).getValue(spelCtx, rootObject, Object.class);
    }

    @Override
    public void setTypeConverters(List<Converter<?, ?>> typeConverters) {
        DefaultConversionService defaultConversionService = new DefaultConversionService();
        typeConverters.forEach(defaultConversionService::addConverter);
        spelCtx.setTypeConverter(new StandardTypeConverter(defaultConversionService));
    }

    @Override
    public void setOperatorOverloader(OperatorOverloader operatorOverloader) {
        if (operatorOverloader != null) {
            spelCtx.setOperatorOverloader(operatorOverloader);
        } else {
            spelCtx.setOperatorOverloader(new EmptyOperatorOverloader());
        }
    }

    @Override
    public void addVariable(String name, Object value) {
        if (name != null) {
            spelCtx.setVariable(name, value);
            if (value != null) {
                variables.put(name, value);
            } else {
                variables.remove(name);
            }
        }
    }

    @Override
    public Map<String, Object> getAllVariables() {
        return new HashMap<>(variables);
    }
}
