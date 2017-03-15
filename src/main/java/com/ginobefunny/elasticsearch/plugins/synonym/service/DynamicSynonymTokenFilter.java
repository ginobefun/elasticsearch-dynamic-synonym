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

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import java.io.IOException;
import java.util.List;

/**
 * Created by ginozhang on 2017/1/12.
 */
public class DynamicSynonymTokenFilter extends TokenFilter {

    public static final String TYPE_SYNONYM = "SYNONYM";

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);

    private final OffsetAttribute offset = addAttribute(OffsetAttribute.class);

    private String currentInput = null;

    private int startOffset = 0;

    private int endOffset = 0;

    private List<String> currentWords = null;

    private int currentIndex = 0;

    public DynamicSynonymTokenFilter(TokenStream input) {
        super(input);
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (currentInput == null) {
            if (!input.incrementToken()) {
                return false;
            }

            currentInput = new String(termAtt.buffer(), 0, termAtt.length());
            startOffset = offset.startOffset();
            endOffset = offset.endOffset();
            currentWords = SynonymRuleManager.getSingleton().getSynonymWords(currentInput);
            if (currentWords == null || currentWords.isEmpty()) {
                currentInput = null;

                // 返回当前的token
                return true;
            }
            currentIndex = 0;
        }

        if (currentIndex >= currentWords.size()) {
            currentInput = null;
            return incrementToken();
        }

        String newWords = currentWords.get(currentIndex);
        currentIndex++;
        clearAttributes();
        char[] output = newWords.toCharArray();
        termAtt.copyBuffer(output, 0, output.length);
        typeAtt.setType(TYPE_SYNONYM);
        offset.setOffset(startOffset, endOffset);
        return true;
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        currentInput = null;
        startOffset = 0;
        endOffset = 0;
        currentWords = null;
        currentIndex = 0;
    }
}
