# UMLVisualizer
UMLVisualizer is a Java-based tool that dynamically generates UML diagrams from Java code. It parses Java files, extracts key information, and displays it in an intuitive, navigable canvas. Designed to streamline code analysis and improve documentation, UMLVisualizer adapts to various project structures with minimal configuration.

## ✨ Key Features
* **Automatic Parsing**: UMLVisualizer scans Java files in the specified directory, extracting class hierarchies, methods, and fields with ease.
* **Interactive Visualization**: Displays UML diagrams with panning, zooming, and smooth movement for enhanced navigation.
* **Customizable Display**: Classes are color-coded based on hierarchy, with abstract classes and subclasses visually connected.

## ⚙️ Installation & Usage
Import the UMLVisualizer package into your project.
Initialize the tool with:
```java
UMLVisualizer.init();
``` 
UMLVisualizer will automatically parse Java files in the directory and onward, rendering a UML diagram on an interactive canvas.

## ☀️ Benefits
UMLVisualizer provides developers with a user-friendly way to visualize class structures and relationships, making code reviews and design discussions **10x** easier and faster. By providing a clear visual representation, it supports faster onboarding, code comprehension, and project documentation.

## 📸 Screenshots
<img width="956" height="506" alt="Screenshot 2025-09-12 at 1 47 52 PM" src="https://github.com/user-attachments/assets/77582e6d-1018-4206-8c96-25ec4147c11d" />
<i>Multiple classes shown on the canvas</i>

<img width="955" height="499" alt="Screenshot 2025-09-12 at 1 48 50 PM" src="https://github.com/user-attachments/assets/c5f1a6d9-9412-4b58-98f0-7a5fed8598e3" />
<i>Example of how inheritance is shown</i>
