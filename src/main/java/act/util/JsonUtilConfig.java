package act.util;

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

import static com.alibaba.fastjson.JSON.DEFAULT_GENERATE_FEATURE;

import act.Act;
import act.app.App;
import act.cli.util.MappedFastJsonNameFilter;
import act.conf.AppConfig;
import act.data.DataPropertyRepository;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.*;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;
import org.osgl.mvc.MvcConfig;
import org.osgl.storage.ISObject;
import org.osgl.storage.impl.SObject;
import org.osgl.util.*;

import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class JsonUtilConfig {

    public static class JsonWriter extends $.Visitor<Writer> {

        private final Object v;
        private SerializerFeature[] features;
        private SerializeFilter[] filters;
        private DateFormat dateFormat;
        private boolean disableCircularReferenceDetect;
        private boolean hasPropFilter;

        public JsonWriter(Object v, PropertySpec.MetaInfo spec, boolean format, ActContext context) {
            if (null == v) {
                this.v = "{}";
            } else if (v instanceof String) {
                String s = S.string(v).trim();
                int len = s.length();
                if (0 == len) {
                    this.v = "{}";
                } else {
                    char a = s.charAt(0);
                    char z = s.charAt(len - 1);
                    if (('{' == a && '}' == z) || ('[' == a && ']' == z)) {
                        this.v = s;
                    } else {
                        this.v = "{\"result\":" + s + "}";
                    }
                }
            } else {
                this.v = v;
                AppConfig config = Act.appConfig();
                Locale locale = null == context ? config.locale() : context.locale(true);
                String dateFormatPattern = null == context ? null : context.dateFormatPattern();
                if (S.blank(dateFormatPattern)) {
                    if (!config.i18nEnabled() || locale.equals(config.locale())) {
                        this.dateFormat = config.dateTimeFormat();
                    } else {
                        dateFormatPattern = config.localizedDateTimePattern(locale);
                        this.dateFormat = new SimpleDateFormat(dateFormatPattern, locale);
                    }
                } else {
                    this.dateFormat = new SimpleDateFormat(dateFormatPattern, locale);
                }
                this.filters = initFilters(v, spec, context);
                this.features = initFeatures(format, context);
            }
        }

        public void disableCircularReferenceDetect() {
            disableCircularReferenceDetect = true;
        }

        private SerializeFilter[] initFilters(Object v, PropertySpec.MetaInfo spec, ActContext context) {
            Set<SerializeFilter> filterSet = new LinkedHashSet<>();
            FastJsonPropertyPreFilter propertyFilter = initPropertyPreFilter(v, spec, context);
            if (null != propertyFilter) {
                hasPropFilter = true;
            }
            if (null != spec && null != context) {
                MappedFastJsonNameFilter nameFilter = new MappedFastJsonNameFilter(spec.labelMapping(context));
                filterSet.add(nameFilter);
            }

            if (null != context) {
                SerializeFilter[] filters = context.fastjsonFilters();
                if (null != filters) {
                    for (SerializeFilter f : filters) {
                        filterSet.add(f);
                    }
                }
            }
            if (null != propertyFilter) {
                filterSet.add(propertyFilter);
            }
            return filterSet.toArray(new SerializeFilter[filterSet.size()]);
        }

        private SerializerFeature[] initFeatures(boolean format, ActContext context) {
            Set<SerializerFeature> featureSet = new HashSet<>();
            if (format) {
                featureSet.add(SerializerFeature.PrettyFormat);
            }
            if (null != context) {
                SerializerFeature[] features = context.fastjsonFeatures();
                if (null != features) {
                    for (SerializerFeature f : features) {
                        featureSet.add(f);
                    }
                }
            }
            if (disableCircularReferenceDetect) {
                featureSet.add(SerializerFeature.DisableCircularReferenceDetect);
            }
            featureSet.add(SerializerFeature.WriteDateUseDateFormat);
            return featureSet.toArray(new SerializerFeature[featureSet.size()]);
        }

        private FastJsonPropertyPreFilter initPropertyPreFilter(Object v, PropertySpec.MetaInfo spec, ActContext context) {
            if (null != context) {
                spec = PropertySpec.MetaInfo.withCurrent(spec, context);
            }
            if (null == spec) {
                return null;
            }
            List<String> outputs = spec.outputFields(context);
            Set<String> excluded = spec.excludedFields(context);
            if (outputs.isEmpty() && excluded.isEmpty()) {
                return null;
            }
            return new FastJsonPropertyPreFilter(v.getClass(), outputs, excluded, context.app().service(DataPropertyRepository.class));
        }

        @Override
        public void visit(Writer writer) throws $.Break {
            if (v instanceof CharSequence) {
                IO.write((CharSequence) v, writer);
                return;
            }
            writeJson(writer, v, SerializeConfig.globalInstance, filters, dateFormat, DEFAULT_GENERATE_FEATURE, features);
        }

        public $.Func0<String> asContentProducer() {
            final JsonWriter me = this;
            return new $.Func0<String>() {
                @Override
                public String apply() throws NotAppliedException, $.Break {
                    S.Buffer buf = S.buffer();
                    me.visit(buf);
                    return buf.toString();
                }
            };
        }
    }

    public static void configure(final App app) {
        SerializeConfig config = SerializeConfig.getGlobalInstance();

        // patch https://github.com/alibaba/fastjson/issues/478
        config.put(FastJsonIterable.class, FastJsonIterableSerializer.instance);

        FastJsonJodaDateCodec jodaDateCodec = new FastJsonJodaDateCodec(app);
        app.registerSingleton(FastJsonJodaDateCodec.class, jodaDateCodec);

        FastJsonValueObjectSerializer valueObjectSerializer = new FastJsonValueObjectSerializer();
        app.registerSingleton(FastJsonValueObjectSerializer.class, valueObjectSerializer);
        FastJsonKeywordCodec keywordCodec = new FastJsonKeywordCodec();
        FastJsonSObjectCodec sObjectCodec = new FastJsonSObjectCodec();

        config.put(DateTime.class, jodaDateCodec);
        config.put(LocalDate.class, jodaDateCodec);
        config.put(LocalTime.class, jodaDateCodec);
        config.put(LocalDateTime.class, jodaDateCodec);
        config.put(ValueObject.class, valueObjectSerializer);
        config.put(Keyword.class, keywordCodec);
        config.put(KV.class, FastJsonKvCodec.INSTANCE);
        config.put(KVStore.class, FastJsonKvCodec.INSTANCE);

        ParserConfig parserConfig = ParserConfig.getGlobalInstance();
        parserConfig.putDeserializer(DateTime.class, jodaDateCodec);
        parserConfig.putDeserializer(LocalDate.class, jodaDateCodec);
        parserConfig.putDeserializer(LocalTime.class, jodaDateCodec);
        parserConfig.putDeserializer(LocalDateTime.class, jodaDateCodec);
        parserConfig.putDeserializer(Keyword.class, keywordCodec);
        parserConfig.putDeserializer(KV.class, FastJsonKvCodec.INSTANCE);
        parserConfig.putDeserializer(KVStore.class, FastJsonKvCodec.INSTANCE);
        parserConfig.putDeserializer(ISObject.class, sObjectCodec);
        parserConfig.putDeserializer(SObject.class, sObjectCodec);

        MvcConfig.jsonSerializer(new $.Func2<Writer, Object, Void>() {
            @Override
            public Void apply(Writer writer, Object v) throws NotAppliedException, $.Break {
                new JsonWriter(v, null, false, ActContext.Base.currentContext()).apply(writer);
                return null;
            }
        });
    }

    // FastJSON does not provide the API so we have to create our own
    private static final void writeJson(Writer os, //
                                        Object object, //
                                        SerializeConfig config, //
                                        SerializeFilter[] filters, //
                                        DateFormat dateFormat, //
                                        int defaultFeatures, //
                                        SerializerFeature... features) {
        SerializeWriter writer = new SerializeWriter(os, defaultFeatures, features);

        try {
            JSONSerializer serializer = new JSONSerializer(writer, config);

            if (dateFormat != null) {
                serializer.setDateFormat(dateFormat);
                serializer.config(SerializerFeature.WriteDateUseDateFormat, true);
            }

            if (filters != null) {
                for (SerializeFilter filter : filters) {
                    serializer.addFilter(filter);
                }
            }
            serializer.write(object);
        } finally {
            writer.close();
        }
    }
}
