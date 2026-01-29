# Yigo Layout Visualizer - Final Walkthrough

This plugin provides a real-time visualizer for Yigo Form XML files within IntelliJ IDEA. It renders a structural preview of the form, supporting complex grid layouts, embedded components, and bidirectional synchronization with the code editor.

## Key Features

### 1. Visual Layout Rendering
- **Grid Layouts**: Correcly mimics `GridLayoutPanel` and `Grid` configurations, including row/column spans (ColSpan/RowSpan).
- **Embedded Forms**: Recursively renders `Embed` components by resolving `FormKey` references and loading definitions in the background.
- **Components**: Renders native-looking placeholders for Yigo components (TextEditor, Dict, etc.) with rounded corners, theme-aware colors, and clear captions.
- **Icons**: Displays distinct icons for each component type (e.g., CheckBox, ComboBox, TextEditor) to improve readability.
- **Hidden Items**: Respects `Visible="false"` (mostly) but ensures structural containers are still processed where necessary.

### 2. Live Synchronization
- **Code-to-Visual**: Visualizer updates instantly when XML changes (via PSI listeners). Highlights the component currently under the editor caret.
- **Visual-to-Code**: Clicking a component in the visualizer navigates to the corresponding tag in the XML file.
- **Drag & Drop**: Supports moving Grid Columns and Cells via drag-and-drop within the visualizer, updating multiple XML tags atomically.

### 3. Search & Navigation
- **Find Component**: `Ctrl+F` (or `Cmd+F`) opens a search bar to filter components by Key or Caption.
- **Keyboard Nav**: `Enter` / `Shift+Enter` to cycle through matches.
- **Smart Focus**: 
    - Auto-navigates if the match is in the *current* file.
    - Requires manual click if the match is in an *embedded* form (to strictly preserve editor context).

## Technical Implementation Highlights

- **`YigoLayoutPanel`**: The core UI component using pure Swing with `JBUI` scaling and `JBColor` for a native look.
- **Async Loading**: Use `ReadAction.nonBlocking` with a `coalesceBy` throttling queue to load embedded forms without freezing the UI or crashing the IDE with too many threads.
- **Thread Safety**: All PSI access is strictly wrapped in `runReadAction` to prevent EDT violation exceptions, ensuring stability during high-frequency updates.
- **Performance**:
    - **O(1) Lookups**: Uses `putClientProperty` to tag Swing components with their XML tags, ensuring instant hit-testing during drag-and-drop.
    - **Atomic PSI Updates**: `moveColumnAndCells` performs surgical PSI edits (add/delete) instead of full re-parses, eliminating flicker.

## Status
All planned features are complete. The visualizer is stable, performant, and follows modern IntelliJ UI guidelines.
