package org.iotp.analytics.ruleengine.core.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.iotp.analytics.ruleengine.common.msg.core.BasicRequest;
import org.iotp.analytics.ruleengine.common.msg.core.BasicTelemetryUploadRequest;
import org.iotp.analytics.ruleengine.common.msg.core.BasicUpdateAttributesRequest;
import org.iotp.analytics.ruleengine.common.msg.core.TelemetryUploadRequest;
import org.iotp.analytics.ruleengine.common.msg.core.ToDeviceRpcRequestMsg;
import org.iotp.analytics.ruleengine.common.msg.core.ToServerRpcRequestMsg;
import org.iotp.analytics.ruleengine.common.msg.core.ToServerRpcResponseMsg;
import org.iotp.analytics.ruleengine.common.msg.core.UpdateAttributesRequest;
import org.iotp.analytics.ruleengine.common.msg.kv.AttributesKVMsg;
import org.iotp.infomgt.data.kv.AttributeKey;
import org.iotp.infomgt.data.kv.AttributeKvEntry;
import org.iotp.infomgt.data.kv.BaseAttributeKvEntry;
import org.iotp.infomgt.data.kv.BooleanDataEntry;
import org.iotp.infomgt.data.kv.DoubleDataEntry;
import org.iotp.infomgt.data.kv.KvEntry;
import org.iotp.infomgt.data.kv.LongDataEntry;
import org.iotp.infomgt.data.kv.StringDataEntry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

public class JsonConverter {

  private static final Gson GSON = new Gson();

  public static TelemetryUploadRequest convertToTelemetry(JsonElement jsonObject) throws JsonSyntaxException {
    return convertToTelemetry(jsonObject, BasicRequest.DEFAULT_REQUEST_ID);
  }

  public static TelemetryUploadRequest convertToTelemetry(JsonElement jsonObject, int requestId)
      throws JsonSyntaxException {
    BasicTelemetryUploadRequest request = new BasicTelemetryUploadRequest(requestId);
    long systemTs = System.currentTimeMillis();
    if (jsonObject.isJsonObject()) {
      parseObject(request, systemTs, jsonObject);
    } else if (jsonObject.isJsonArray()) {
      jsonObject.getAsJsonArray().forEach(je -> {
        if (je.isJsonObject()) {
          parseObject(request, systemTs, je.getAsJsonObject());
        } else {
          throw new JsonSyntaxException("Can't parse value: " + je);
        }
      });
    } else {
      throw new JsonSyntaxException("Can't parse value: " + jsonObject);
    }
    return request;
  }

  public static ToServerRpcRequestMsg convertToServerRpcRequest(JsonElement json, int requestId)
      throws JsonSyntaxException {
    JsonObject object = json.getAsJsonObject();
    return new ToServerRpcRequestMsg(requestId, object.get("method").getAsString(), GSON.toJson(object.get("params")));
  }

  private static void parseObject(BasicTelemetryUploadRequest request, long systemTs, JsonElement jsonObject) {
    JsonObject jo = jsonObject.getAsJsonObject();
    if (jo.has("ts") && jo.has("values")) {
      parseWithTs(request, jo);
    } else {
      parseWithoutTs(request, systemTs, jo);
    }
  }

  private static void parseWithoutTs(BasicTelemetryUploadRequest request, long systemTs, JsonObject jo) {
    for (KvEntry entry : parseValues(jo)) {
      request.add(systemTs, entry);
    }
  }

  public static void parseWithTs(BasicTelemetryUploadRequest request, JsonObject jo) {
    long ts = jo.get("ts").getAsLong();
    JsonObject valuesObject = jo.get("values").getAsJsonObject();
    for (KvEntry entry : parseValues(valuesObject)) {
      request.add(ts, entry);
    }
  }

  public static List<KvEntry> parseValues(JsonObject valuesObject) {
    List<KvEntry> result = new ArrayList<>();
    for (Entry<String, JsonElement> valueEntry : valuesObject.entrySet()) {
      JsonElement element = valueEntry.getValue();
      if (element.isJsonPrimitive()) {
        JsonPrimitive value = element.getAsJsonPrimitive();
        if (value.isString()) {
          result.add(new StringDataEntry(valueEntry.getKey(), value.getAsString()));
        } else if (value.isBoolean()) {
          result.add(new BooleanDataEntry(valueEntry.getKey(), value.getAsBoolean()));
        } else if (value.isNumber()) {
          if (value.getAsString().contains(".")) {
            result.add(new DoubleDataEntry(valueEntry.getKey(), value.getAsDouble()));
          } else {
            result.add(new LongDataEntry(valueEntry.getKey(), value.getAsLong()));
          }
        } else {
          throw new JsonSyntaxException("Can't parse value: " + value);
        }
      } else {
        throw new JsonSyntaxException("Can't parse value: " + element);
      }
    }
    return result;
  }

  public static UpdateAttributesRequest convertToAttributes(JsonElement element) {
    return convertToAttributes(element, BasicRequest.DEFAULT_REQUEST_ID);
  }

  public static UpdateAttributesRequest convertToAttributes(JsonElement element, int requestId) {
    if (element.isJsonObject()) {
      BasicUpdateAttributesRequest request = new BasicUpdateAttributesRequest(requestId);
      long ts = System.currentTimeMillis();
      request.add(parseValues(element.getAsJsonObject()).stream().map(kv -> new BaseAttributeKvEntry(kv, ts))
          .collect(Collectors.toList()));
      return request;
    } else {
      throw new JsonSyntaxException("Can't parse value: " + element);
    }
  }

  public static JsonObject toJson(AttributesKVMsg payload, boolean asMap) {
    JsonObject result = new JsonObject();
    if (asMap) {
      if (!payload.getClientAttributes().isEmpty()) {
        JsonObject attrObject = new JsonObject();
        payload.getClientAttributes().forEach(addToObject(attrObject));
        result.add("client", attrObject);
      }
      if (!payload.getSharedAttributes().isEmpty()) {
        JsonObject attrObject = new JsonObject();
        payload.getSharedAttributes().forEach(addToObject(attrObject));
        result.add("shared", attrObject);
      }
    } else {
      payload.getClientAttributes().forEach(addToObject(result));
      payload.getSharedAttributes().forEach(addToObject(result));
    }
    if (!payload.getDeletedAttributes().isEmpty()) {
      JsonArray attrObject = new JsonArray();
      payload.getDeletedAttributes().forEach(addToObject(attrObject));
      result.add("deleted", attrObject);
    }
    return result;
  }

  private static Consumer<AttributeKey> addToObject(JsonArray result) {
    return key -> {
      result.add(key.getAttributeKey());
    };
  }

  private static Consumer<AttributeKvEntry> addToObject(JsonObject result) {
    return de -> {
      JsonPrimitive value;
      switch (de.getDataType()) {
      case BOOLEAN:
        value = new JsonPrimitive(de.getBooleanValue().get());
        break;
      case DOUBLE:
        value = new JsonPrimitive(de.getDoubleValue().get());
        break;
      case LONG:
        value = new JsonPrimitive(de.getLongValue().get());
        break;
      case STRING:
        value = new JsonPrimitive(de.getStrValue().get());
        break;
      default:
        throw new IllegalArgumentException("Unsupported data type: " + de.getDataType());
      }
      result.add(de.getKey(), value);
    };
  }

  public static JsonObject toJson(ToDeviceRpcRequestMsg msg, boolean includeRequestId) {
    JsonObject result = new JsonObject();
    if (includeRequestId) {
      result.addProperty("id", msg.getRequestId());
    }
    result.addProperty("method", msg.getMethod());
    result.add("params", new JsonParser().parse(msg.getParams()));
    return result;
  }

  public static JsonElement toJson(ToServerRpcResponseMsg msg) {
    return new JsonParser().parse(msg.getData());
  }

  public static JsonElement toErrorJson(String errorMsg) {
    JsonObject error = new JsonObject();
    error.addProperty("error", errorMsg);
    return error;
  }
}
