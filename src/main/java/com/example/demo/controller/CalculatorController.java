package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class CalculatorController {

    @Autowired
    private CalculationRepository calculationRepository;

    @MessageMapping("/calculate")
    @SendTo("/topic/calculator")
    public CalculatorMessage calculate(CalculatorMessage message) {
        double res = 0;
        if (message.getOperator() != null) {
            switch (message.getOperator()) {
                case "+":
                    res = message.getNum1() + message.getNum2();
                    break;
                case "-":
                    res = message.getNum1() - message.getNum2();
                    break;
                case "*":
                    res = message.getNum1() * message.getNum2();
                    break;
                case "/":
                    if (message.getNum2() != 0) {
                        res = message.getNum1() / message.getNum2();
                    }
                    break;
            }
        }
        message.setResult(res);

        // Save data into MySQL database
        CalculationHistory history = new CalculationHistory();
        history.setSender(message.getSender());
        history.setNum1(message.getNum1());
        history.setNum2(message.getNum2());
        history.setOperator(message.getOperator());
        history.setResult(res);

        calculationRepository.save(history);

        return message;
    }
}