package cn.lyp.agent.tools;

import cn.lyp.agent.Tool;
import cn.lyp.agent.ToolResult;

import java.util.Collections;
import java.util.Map;
import java.util.Stack;

public class CalcTool implements Tool {
    @Override
    public String name() {
        return "calc";
    }

    @Override
    public String description() {
        return "Evaluate a basic math expression with +, -, *, /, parentheses.";
    }

    @Override
    public Map<String, String> args() {
        return Collections.singletonMap("expression", "string");
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        Object expr = args.get("expression");
        if (expr == null) {
            return ToolResult.error("Missing argument: expression");
        }
        try {
            double value = evaluate(expr.toString());
            return ToolResult.ok(Double.toString(value));
        } catch (IllegalArgumentException ex) {
            return ToolResult.error("Invalid expression: " + ex.getMessage());
        }
    }

    private double evaluate(String expression) {
        if (expression == null) {
            throw new IllegalArgumentException("expression is null");
        }
        String exp = expression.replaceAll("\\s+", "");
        if (exp.isEmpty()) {
            throw new IllegalArgumentException("expression is empty");
        }

        Stack<Double> values = new Stack<>();
        Stack<Character> ops = new Stack<>();
        int i = 0;
        while (i < exp.length()) {
            char ch = exp.charAt(i);
            if (Character.isDigit(ch) || ch == '.') {
                int start = i;
                while (i < exp.length()) {
                    char c = exp.charAt(i);
                    if (!Character.isDigit(c) && c != '.') {
                        break;
                    }
                    i++;
                }
                values.push(Double.parseDouble(exp.substring(start, i)));
                continue;
            }
            if (ch == '(') {
                ops.push(ch);
            } else if (ch == ')') {
                while (!ops.isEmpty() && ops.peek() != '(') {
                    applyOp(values, ops.pop());
                }
                if (ops.isEmpty() || ops.pop() != '(') {
                    throw new IllegalArgumentException("mismatched parentheses");
                }
            } else if (isOperator(ch)) {
                while (!ops.isEmpty() && precedence(ops.peek()) >= precedence(ch)) {
                    applyOp(values, ops.pop());
                }
                ops.push(ch);
            } else {
                throw new IllegalArgumentException("unexpected character: " + ch);
            }
            i++;
        }

        while (!ops.isEmpty()) {
            applyOp(values, ops.pop());
        }

        if (values.size() != 1) {
            throw new IllegalArgumentException("invalid expression");
        }
        return values.pop();
    }

    private boolean isOperator(char ch) {
        return ch == '+' || ch == '-' || ch == '*' || ch == '/';
    }

    private int precedence(char op) {
        if (op == '+' || op == '-') {
            return 1;
        }
        if (op == '*' || op == '/') {
            return 2;
        }
        return 0;
    }

    private void applyOp(Stack<Double> values, char op) {
        if (values.size() < 2) {
            throw new IllegalArgumentException("insufficient values");
        }
        double b = values.pop();
        double a = values.pop();
        double result;
        switch (op) {
            case '+':
                result = a + b;
                break;
            case '-':
                result = a - b;
                break;
            case '*':
                result = a * b;
                break;
            case '/':
                if (b == 0) {
                    throw new IllegalArgumentException("division by zero");
                }
                result = a / b;
                break;
            default:
                throw new IllegalArgumentException("unknown operator: " + op);
        }
        values.push(result);
    }
}
