package com.flipkart.krystal.krystalrelease;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public final class RepoInfo {
  private List<ProjectInfo> projects = new ArrayList<>();
}
