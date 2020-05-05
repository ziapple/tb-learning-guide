/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ziapple.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ziapple.common.data.BaseData;
import com.ziapple.common.data.id.EntityId;
import com.ziapple.common.data.id.TenantId;
import com.ziapple.common.data.id.UUIDBased;
import com.ziapple.service.device.DeviceService;
import com.ziapple.service.entityview.EntityViewService;
import com.ziapple.service.tenant.TenantService;
import com.ziapple.service.timeseries.TimeseriesService;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.io.IOException;
import java.util.Comparator;


@RunWith(SpringRunner.class)
@ContextConfiguration(classes = AbstractServiceTest.class, loader = AnnotationConfigContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Configuration
@ComponentScan({"com.ziapple.service","com.ziapple.dao"})
public abstract class AbstractServiceTest {
    protected ObjectMapper mapper = new ObjectMapper();

    public static final TenantId SYSTEM_TENANT_ID = new TenantId(EntityId.NULL_UUID);

    @Autowired
    protected DeviceService deviceService;

    @Autowired
    protected TenantService tenantService;

    @Autowired
    protected TimeseriesService tsService;

    @Autowired
    protected EntityViewService entityViewService;

    class IdComparator<D extends BaseData<? extends UUIDBased>> implements Comparator<D> {
        @Override
        public int compare(D o1, D o2) {
            return o1.getId().getId().compareTo(o2.getId().getId());
        }
    }


    public JsonNode readFromResource(String resourceName) throws IOException {
        return mapper.readTree(this.getClass().getClassLoader().getResourceAsStream(resourceName));
    }
}
