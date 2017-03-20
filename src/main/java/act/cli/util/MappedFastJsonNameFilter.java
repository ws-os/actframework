package act.cli.util;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.alibaba.fastjson.serializer.NameFilter;
import org.osgl.util.C;

import java.util.Map;

public class MappedFastJsonNameFilter implements NameFilter {

    private Map<String, String> nameMaps = C.newMap();

    /**
     * Construct a {@code MappedFastJsonNameFilter} with prop-name mapping
     * @param mapping the mapping from property to property name (label)
     */
    public MappedFastJsonNameFilter(Map<String, String> mapping) {
        nameMaps.putAll(mapping);
    }

    @Override
    public String process(Object object, String name, Object value) {
        String label = nameMaps.get(name);
        return null == label ? name : label;
    }

    public void addMap(String property, String label) {
        nameMaps.put(property, label);
    }

    public boolean isEmpty() {
        return nameMaps.isEmpty();
    }
}
