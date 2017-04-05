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
package com.ginobefunny.elasticsearch.plugins.synonym;

import com.ginobefunny.elasticsearch.plugins.synonym.service.Configuration;
import com.ginobefunny.elasticsearch.plugins.synonym.service.DynamicSynonymTokenFilter;
import com.ginobefunny.elasticsearch.plugins.synonym.service.SynonymRuleManager;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AbstractTokenFilterFactory;

import java.io.IOException;

public class DynamicSynonymTokenFilterFactory extends AbstractTokenFilterFactory {

    @Inject
    public DynamicSynonymTokenFilterFactory(IndexSettings indexSettings, Environment env,
                                            String name, Settings settings) throws IOException {
        super(indexSettings, name, settings);

        // get the filter setting params
        final boolean ignoreCase = settings.getAsBoolean("ignore_case", false);
        final boolean expand = settings.getAsBoolean("expand", true);
        String dbUrl = settings.get("db_url");

        String tokenizerName = settings.get("tokenizer", "whitespace");
        Analyzer analyzer;
        if ("standand".equalsIgnoreCase(tokenizerName)) {
            analyzer = new StandardAnalyzer();
        } else if ("keyword".equalsIgnoreCase(tokenizerName)) {
            analyzer = new KeywordAnalyzer();
        } else if ("simple".equalsIgnoreCase(tokenizerName)) {
            analyzer = new SimpleAnalyzer();
        } else {
            analyzer = new WhitespaceAnalyzer();
        }

        // NOTE: the manager will only init once
        SynonymRuleManager.initial(new Configuration(ignoreCase, expand, analyzer, dbUrl));
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new DynamicSynonymTokenFilter(tokenStream);
    }
}
