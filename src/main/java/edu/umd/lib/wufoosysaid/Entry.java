package edu.umd.lib.wufoosysaid;

import java.io.Serializable;
import java.util.Map;

public class Entry implements Serializable {
  /**
   * 
   */

  private static final long serialVersionUID = 1L;
  private String hash;
  private String entryId;
  private Map<String, String> fields;

  public Entry() {
  }

  public Entry(String formHash, String id, Map<String, String> fieldsMap) {
    hash = formHash;
    entryId = id;
    fields = fieldsMap;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public String getEntryId() {
    return entryId;
  }

  public void setEntryId(String entryId) {
    this.entryId = entryId;
  }

  public Map<String, String> getFields() {
    return fields;
  }

  public void setFields(Map<String, String> fields) {
    this.fields = fields;
  }
}
