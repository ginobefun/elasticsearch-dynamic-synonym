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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import java.io.IOException;
import java.util.*;

/**
 * Created by ginozhang on 2017/1/12.
 */
public class SimpleSynonymMap {

    private static final ESLogger logger = Loggers.getLogger("dynamic-synonym");

    private Map<String, List<String>> ruleMap = new HashMap<String, List<String>>();

    private Configuration configuration;

    public SimpleSynonymMap(Configuration cfg) {
        this.configuration = cfg;
    }

    public void addRule(String rule) {
        try {
            addInternal(rule);
        } catch (Throwable t) {
            logger.error("Add synonym rule failed. rule: " + rule, t);
        }
    }

    private void addInternal(String line) throws IOException {
        String sides[] = split(line, "=>");
        if (sides.length > 1) { // explicit mapping
            if (sides.length != 2) {
                throw new IllegalArgumentException("more than one explicit mapping specified on the same line");
            }

            Set<String> inputStrSet = new HashSet<String>();
            String inputStrings[] = split(sides[0], ",");
            for (int i = 0; i < inputStrings.length; i++) {
                inputStrSet.addAll(analyze(process(inputStrings[i])));
            }

            Set<String> outputStrSet = new HashSet<String>();
            String outputStrings[] = split(sides[1], ",");
            for (int i = 0; i < outputStrings.length; i++) {
                outputStrSet.addAll(analyze(process(outputStrings[i])));
            }

            // these mappings are explicit and never preserve original
            for (String input : inputStrSet) {
                for (String output : outputStrSet) {
                    addToRuleMap(input, output);
                }
            }
        } else {
            Set<String> inputStrSet = new HashSet<String>();
            String inputStrings[] = split(line, ",");
            for (int i = 0; i < inputStrings.length; i++) {
                inputStrSet.addAll(analyze(process(inputStrings[i])));
            }

            // use expand=true to get all pairs
            for (String input : inputStrSet) {
                for (String output : inputStrSet) {
                    addToRuleMap(input, output);
                }
            }
        }
    }

    private Set<String> analyze(String text) throws IOException {
        Set<String> result = new HashSet<String>();
        Analyzer analyzer = configuration.getAnalyzer();
        TokenStream ts = analyzer.tokenStream("", text);
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
        String lowercaseStr = input.trim().toLowerCase(Locale.getDefault());
        if (lowercaseStr.indexOf("\\") >= 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lowercaseStr.length(); i++) {
                char ch = lowercaseStr.charAt(i);
                if (ch == '\\' && i < lowercaseStr.length() - 1) {
                    sb.append(lowercaseStr.charAt(++i));
                } else {
                    sb.append(ch);
                }
            }
            return sb.toString();
        }
        return lowercaseStr;
    }

    public List<String> getSynonymWords(String input) {
        if (!ruleMap.containsKey(input)) {
            return null;
        }

        return ruleMap.get(input);
    }

}
