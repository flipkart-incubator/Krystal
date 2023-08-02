package com.flipkart.krystal.mojo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public final class ProjectInfo {
  private String name;
  private final List<ProjectStageInfo> stageInfos = new ArrayList<>();

  public Optional<ProjectStageInfo> getStageInfo(PublishStage stage) {
    return stageInfos.stream().filter(stageInfo -> stage.equals(stageInfo.getStage())).findAny();
  }
}
