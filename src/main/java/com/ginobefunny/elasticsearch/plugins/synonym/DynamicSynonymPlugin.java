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

import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.index.analysis.AnalysisModule;
import org.elasticsearch.plugins.Plugin;

import java.util.Collection;
import java.util.Collections;

/**
 * Created by ginozhang on 2017/1/12.
 */
public class DynamicSynonymPlugin extends Plugin {

    /** Plugin name **/
    public static final String PLUGIN_NAME = "dynamic-synonym";

    public static final ESLogger logger = ESLoggerFactory.getLogger(PLUGIN_NAME);

    @Override
    public String name() {
        return PLUGIN_NAME;
    }

    @Override
    public String description() {
        return "ElasticSearch Plugin for Dynaic Synonym Token Filter.";
    }

    @Override
    public Collection<Module> nodeModules() {
        // this method will be called when node start
        return Collections.<Module>singletonList(new DynamicSynonymModule());
    }

    public void onModule(AnalysisModule module) {
        // this mothod will be called where analyzer need
        module.addProcessor(new DynamicSynonymBinderProcessor());
    }
}