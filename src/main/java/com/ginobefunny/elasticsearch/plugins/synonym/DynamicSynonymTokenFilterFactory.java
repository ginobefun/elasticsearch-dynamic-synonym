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
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.AbstractTokenFilterFactory;
import org.elasticsearch.index.analysis.AnalysisSettingsRequired;
import org.elasticsearch.index.analysis.TokenizerFactory;
import org.elasticsearch.index.analysis.TokenizerFactoryFactory;
import org.elasticsearch.index.settings.IndexSettingsService;
import org.elasticsearch.indices.analysis.IndicesAnalysisService;

import java.util.Map;

@AnalysisSettingsRequired
public class DynamicSynonymTokenFilterFactory extends AbstractTokenFilterFactory {

    @Inject
    public DynamicSynonymTokenFilterFactory(Index index, IndexSettingsService indexSettingsService, Environment env, IndicesAnalysisService indicesAnalysisService, Map<String, TokenizerFactoryFactory> tokenizerFactories,
                                            @Assisted String name, @Assisted Settings settings) {
        super(index, indexSettingsService.getSettings(), name, settings);

        // get the filter setting params
        final boolean ignoreCase = settings.getAsBoolean("ignore_case", false);
        final boolean expand = settings.getAsBoolean("expand", true);
        String dbUrl = settings.get("db_url");
        String tokenizerName = settings.get("tokenizer", "whitespace");
        DynamicSynonymPlugin.logger.debug("tokenizer = {}, db_url = {},  ignore_case = {}, expand = {}", tokenizerName, dbUrl, ignoreCase, expand);

        TokenizerFactoryFactory tokenizerFactoryFactory = tokenizerFactories.get(tokenizerName);
        if (tokenizerFactoryFactory == null) {
            tokenizerFactoryFactory = indicesAnalysisService.tokenizerFactoryFactory(tokenizerName);
        }
        if (tokenizerFactoryFactory == null) {
            throw new IllegalArgumentException("failed to find tokenizer [" + tokenizerName + "] for synonym token filter");
        }

        final TokenizerFactory tokenizerFactory = tokenizerFactoryFactory.create(tokenizerName, Settings.builder().put(indexSettingsService.getSettings()).put(settings).build());
        Analyzer analyzer = new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                Tokenizer tokenizer = tokenizerFactory == null ? new WhitespaceTokenizer() : tokenizerFactory.create();
                TokenStream stream = ignoreCase ? new LowerCaseFilter(tokenizer) : tokenizer;
                return new TokenStreamComponents(tokenizer, stream);
            }
        };

        // NOTE: the manager will only init once
        SynonymRuleManager.initial(new Configuration(ignoreCase, expand, analyzer, dbUrl));
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new DynamicSynonymTokenFilter(tokenStream);
    }
}
