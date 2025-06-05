// AnalysisService.java
package com.example.demo.service;

import com.example.demo.model.TransferData;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class AnalysisService {

    public List<TransferData> generateFakeData(String type) {
        List<TransferData> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            list.add(new TransferData(i, 25 + i, 10 + i, 15 + i));
        }
        return list;
    }
}
