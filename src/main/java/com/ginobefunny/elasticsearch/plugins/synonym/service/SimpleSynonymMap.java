/*
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
package com.ginobefunny.elasticsearch.plugins.synonym.service;

import com.ginobefunny.elasticsearch.plugins.synonym.service.utils.Monitor;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.elasticsearch.common.logging.ESLoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Created by ginozhang on 2017/1/12.
 * SEE: org.apache.lucene.analysis.synonym.SolrSynonymParser
 */
public class SimpleSynonymMap {

    private static final Logger LOGGER = ESLoggerFactory.getLogger(Monitor.class.getName());

    private Map<String, List<String>> ruleMap = new HashMap<String, List<String>>();

    private final Configuration configuration;

    public SimpleSynonymMap(Configuration cfg) {
        this.configuration = cfg;
    }

    public void addRule(String rule) {
        try {
            addInternal(rule);
        } catch (Throwable t) {
            LOGGER.error("Add synonym rule failed. rule: " + rule, t);
        }
    }

    private void addInternal(String line) throws IOException {
        String sides[] = split(line, "=>");
        if (sides.length > 1) { // explicit mapping
            if (sides.length != 2) {
                throw new IllegalArgumentException("more than one explicit mapping specified on the same line");
            }

            List<String> inputList = new ArrayList<>();
            String inputStrings[] = split(sides[0], ",");
            for (int i = 0; i < inputStrings.length; i++) {
                inputList.addAll(analyze(process(inputStrings[i])));
            }

            List<String> outputList = new ArrayList<>();
            String outputStrings[] = split(sides[1], ",");
            for (int i = 0; i < outputStrings.length; i++) {
                outputList.addAll(analyze(process(outputStrings[i])));
            }

            // these mappings are explicit and never preserve original
            for (String input : inputList) {
                for (String output : outputList) {
                    addToRuleMap(input, output);
                }
            }
        } else {
            List<String> inputList = new ArrayList<>();
            String inputStrings[] = split(line, ",");
            for (int i = 0; i < inputStrings.length; i++) {
                inputList.addAll(analyze(process(inputStrings[i])));
            }

            if (configuration.isExpand()) {
                // all pairs
                for (String input : inputList) {
                    for (String output : inputList) {
                        addToRuleMap(input, output);
                    }
                }
            } else {
                // all subsequent inputs map to first one; we also add inputs[0] here
                // so that we "effectively" (because we remove the original input and
                // add back a synonym with the same text) change that token's type to
                // SYNONYM (matching legacy behavior):
                for (int i = 0; i < inputList.size(); i++) {
                    addToRuleMap(inputList.get(i), inputList.get(0));
                }
            }
        }
    }

    private Set<String> analyze(String text) throws IOException {
        Set<String> result = new HashSet<String>();
        Analyzer analyzer = configuration.getAnalyzer();
        try (TokenStream ts = analyzer.tokenStream("", text)) {
            CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
            PositionIncrementAttribute posIncAtt = ts.addAttribute(PositionIncrementAttribute.class);
            ts.reset();
            while (ts.incrementToken()) {
                int length = termAtt.length();
                if (length == 0) {
                    throw new IllegalArgumentException("term: " + text + " analyzed to a zero-length token");
                }
                if (posIncAtt.getPositionIncrement() != 1) {
                    throw new IllegalArgumentException("term: " + text + " analyzed to a token with posinc != 1");
                }

                result.add(new String(termAtt.buffer(), 0, termAtt.length()));
            }

            ts.end();
            return result;
        }
    }

    private void addToRuleMap(String inputString, String outputString) {
        List<String> outputs = ruleMap.get(inputString);
        if (outputs == null) {
            outputs = new ArrayList<String>();
            ruleMap.put(inputString, outputs);
        }

        if (!outputs.contains(outputString)) {
            outputs.add(outputString);
        }
    }

    private static String[] split(String s, String separator) {
        List<String> list = new ArrayList<String>(2);
        StringBuilder sb = new StringBuilder();
        int pos = 0, end = s.length();
        while (pos < end) {
            if (s.startsWith(separator, pos)) {
                if (sb.length() > 0) {
                    list.add(sb.toString());
                    sb = new StringBuilder();
                }
                pos += separator.length();
                continue;
            }

            char ch = s.charAt(pos++);
            if (ch == '\\') {
                sb.append(ch);
                if (pos >= end) break;  // ERROR, or let it go?
                ch = s.charAt(pos++);
            }

            sb.append(ch);
        }

        if (sb.length() > 0) {
            list.add(sb.toString());
        }

        return list.toArray(new String[list.size()]);
    }

    private String process(String input) {

        String inputStr = configuration.isIgnoreCase() ? input.trim().toLowerCase(Locale.getDefault()) : input;
        if (inputStr.indexOf("\\") >= 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < inputStr.length(); i++) {
                char ch = inputStr.charAt(i);
                if (ch == '\\' && i < inputStr.length() - 1) {
                    sb.append(inputStr.charAt(++i));
                } else {
                    sb.append(ch);
                }
            }
            return sb.toString();
        }
        return inputStr;
    }

    public List<String> getSynonymWords(String input) {
        if (!ruleMap.containsKey(input)) {
            return null;
        }

        return ruleMap.get(input);
    }

}
