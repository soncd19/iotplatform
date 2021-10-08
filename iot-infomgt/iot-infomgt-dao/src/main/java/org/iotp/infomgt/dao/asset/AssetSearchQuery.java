/*******************************************************************************
 * Copyright 2017 osswangxining@163.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.iotp.infomgt.dao.asset;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.iotp.infomgt.dao.relation.EntityRelationsQuery;
import org.iotp.infomgt.dao.relation.EntityTypeFilter;
import org.iotp.infomgt.dao.relation.RelationsSearchParameters;
import org.iotp.infomgt.data.common.ThingType;
import org.iotp.infomgt.data.relation.EntityRelation;

import lombok.Data;

/**
 * Created by ashvayka on 03.05.17.
 */
@Data
public class AssetSearchQuery {

    private RelationsSearchParameters parameters;
    @Nullable
    private String relationType;
    @Nullable
    private List<String> assetTypes;

    public EntityRelationsQuery toEntitySearchQuery() {
        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(parameters);
        query.setFilters(
                Collections.singletonList(new EntityTypeFilter(relationType == null ? EntityRelation.CONTAINS_TYPE : relationType,
                        Collections.singletonList(ThingType.ASSET))));
        return query;
    }
}
