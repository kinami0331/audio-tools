package cc.kinami.audiotool.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExperimentParam {
    int samplingRate;
    int lowerLimit;
    int upperLimit;
    int chirpTime;
    int prepareTime;
    double soundSpeed;
    List<String> deviceList;
}
