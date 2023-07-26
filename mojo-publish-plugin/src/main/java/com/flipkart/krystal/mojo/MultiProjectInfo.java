package com.flipkart.krystal.mojo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public final class MultiProjectInfo {
  private List<ProjectInfo> projects = new ArrayList<>();
}
