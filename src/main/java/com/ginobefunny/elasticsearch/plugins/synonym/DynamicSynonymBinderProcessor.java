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

import org.elasticsearch.index.analysis.AnalysisModule;

public class DynamicSynonymBinderProcessor extends AnalysisModule.AnalysisBinderProcessor {

    @Override
    public void processAnalyzers(AnalyzersBindings analyzersBindings) {
    }

    @Override
    public void processTokenizers(TokenizersBindings tokenizersBindings) {
    }

    @Override
    public void processTokenFilters(TokenFiltersBindings tokenFiltersBindings) {
        // bind the filter type with TokenFilterFactory
        tokenFiltersBindings.processTokenFilter(DynamicSynonymPlugin.PLUGIN_NAME, DynamicSynonymTokenFilterFactory.class);
    }
}
