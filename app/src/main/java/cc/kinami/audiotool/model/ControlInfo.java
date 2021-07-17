package cc.kinami.audiotool.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ControlInfo {
    ProcessControlEnum controlInfo;
    Integer experimentId;
    String from;
    String to;
    Integer experimentType;
    Integer mic;
    Integer fs;
}
