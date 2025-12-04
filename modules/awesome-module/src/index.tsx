import AwesomeModule from "./NativeAwesomeModule";

/**
 * Wywołuje natywną funkcję processImage
 * @param path - ścieżka do pliku obrazu
 * @returns tablica detekcji, każda detekcja to [x1, y1, x2, y2, confidence, class]
 */
export function processImage(path: string): Promise<number[][]> {
  return AwesomeModule.processImage(path);
}
