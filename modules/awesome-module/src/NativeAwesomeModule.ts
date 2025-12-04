import { TurboModuleRegistry, type TurboModule } from "react-native";

export interface Spec extends TurboModule {
  /**
   * Process an image at the given file path and return detection results.
   * Each detection is an array of 6 numbers: [x1, y1, x2, y2, confidence, class]
   */
  processImage(path: string): Promise<number[][]>;
}

export default TurboModuleRegistry.getEnforcing<Spec>("AwesomeModule");
