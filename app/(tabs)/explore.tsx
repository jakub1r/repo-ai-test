import { Button, StyleSheet, Text, View } from "react-native";
import { useTensorflowModel } from "react-native-fast-tflite";
import { useSharedValue } from "react-native-reanimated";
import {
  Camera,
  useCameraDevice,
  useCameraPermission,
  useFrameProcessor,
} from "react-native-vision-camera";
import { useResizePlugin } from "vision-camera-resize-plugin";

export default function TabTwoScreen() {
  const objectDetection = useTensorflowModel(
    require("../../assets/models/best_float32.tflite")
  );
  const model =
    objectDetection.state === "loaded" ? objectDetection.model : undefined;
  console.log(model?.outputs);
  const { resize } = useResizePlugin();
  const device = useCameraDevice("back");
  const { hasPermission, requestPermission } = useCameraPermission();
  let frameCount = useSharedValue(true);
  let resizedShared = useSharedValue<null | Float32Array<ArrayBufferLike>>(
    null
  );

  const frameProcessor = useFrameProcessor(
    (frame) => {
      "worklet";

      if (model == null) return;
      // wykonaj tylko co 500 klatek
      if (frameCount.value) {
        return;
      }

      // reset licznika
      frameCount.value = false;

      const resized = resize(frame, {
        scale: { width: 640, height: 640 },
        pixelFormat: "rgb",
        dataType: "float32",
      });
      const outputTensor = model.runSync([resized]);
      const [bb, scores] = outputTensor;
      //console.log(bb);
      const flatObj = outputTensor[0];

      // zamieniamy obiekt na tablicę wartości po indeksach
      const totalValues = Object.keys(flatObj).length; // powinno być ok. 1800
      const valuesPerDet = 6;
      const numDetections = totalValues / valuesPerDet;

      const detections = [];

      for (let i = 0; i < numDetections; i++) {
        const base = i * valuesPerDet;

        const cx = flatObj[base + 0];
        const cy = flatObj[base + 1];
        const bw = flatObj[base + 2];
        const bh = flatObj[base + 3];
        const conf = flatObj[base + 4];
        const cls = flatObj[base + 5];

        if (conf > 0.5) {
          detections.push({
            cx,
            cy,
            bw,
            bh,
            conf,
            cls_id: Math.round(cls),
          });
        }
      }

      console.log("DETECTIONS:", detections);
    },
    [model]
  );

  if (device == null)
    return (
      <View style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
        <Text>asd</Text>
      </View>
    );
  if (!hasPermission)
    return (
      <View style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
        <Button title="XD" onPress={requestPermission}></Button>
      </View>
    );
  return (
    <Camera
      frameProcessor={frameProcessor}
      style={StyleSheet.absoluteFill}
      device={device}
      isActive={true}
    />
  );
}

const styles = StyleSheet.create({});
