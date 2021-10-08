package org.iotp.infomgt.data.id;

import java.util.UUID;

import org.iotp.infomgt.data.common.ThingType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class CustomerId extends UUIDBased implements EntityId {

  private static final long serialVersionUID = 1L;

  @JsonCreator
  public CustomerId(@JsonProperty("id") UUID id) {
    super(id);
  }

  @JsonIgnore
  @Override
  public ThingType getEntityType() {
    return ThingType.CUSTOMER;
  }
}
