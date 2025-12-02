import { useRef, useState } from "react";
import {
  Button,
  NativeModules,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";
import {
  Camera,
  useCameraDevice,
  useCameraPermission,
} from "react-native-vision-camera";

export default function TabTwoScreen() {
  const device = useCameraDevice("back");
  const cameraRef = useRef<Camera>(null);
  const SDCARDPATH = "/sdcard/Download/xx.jpg";
  const [predictions, setPredictions] = useState<number[]>([]);
  // Ścieżka źródłowa w projekcie
  // Ścieżka docelowa w cache aplikacji

  // Kopiowanie

  const { hasPermission, requestPermission } = useCameraPermission();
  const { SimpleMath } = NativeModules;
  const takePhoto = async () => {
    setPredictions((pred) => []);
    const photo = await cameraRef.current?.takePhoto();
    console.log(photo?.path);
    await SimpleMath.processImage(photo?.path).then((result: []) => {
      result.forEach((element) => {
        setPredictions((pred) => [...pred, element[5]]);
      });
    });
  };

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
    <View style={[StyleSheet.absoluteFill, { backgroundColor: "red" }]}>
      <Camera
        ref={cameraRef}
        style={StyleSheet.absoluteFill}
        device={device}
        isActive={true}
        photo={true}
      />
      <View
        style={{
          position: "absolute",
          backgroundColor: "red",
          width: "100%",
          flexDirection: "row",
          justifyContent: "space-evenly",
        }}
      >
        {predictions.map((element, index) => (
          <Text style={{ fontSize: 25, paddingTop: 50 }}>{element}</Text>
        ))}
      </View>
      <TouchableOpacity
        onPress={takePhoto}
        style={[styles.takePhotoBtn]}
      ></TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  takePhotoBtn: {
    position: "absolute",
    alignSelf: "center",
    bottom: 10,
    borderWidth: 5,
    borderRadius: 50,
    borderColor: "white",
    height: 75,
    width: 75,
  },
});
