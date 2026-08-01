from __future__ import annotations

import argparse
from pathlib import Path
import sys
import tempfile

from .bnfo_adapter import load_bnfo_from_path
from .layout import build_render_tree
from .svg_render import render_svg


def _import_qt() -> tuple[object, object, object, object, object, object, object, object, object, object, object]:
    try:
        from PySide6.QtCore import QByteArray, QRectF, Qt
        from PySide6.QtGui import QAction
        from PySide6.QtSvg import QSvgRenderer
        from PySide6.QtWidgets import QApplication, QFileDialog, QLabel, QMainWindow, QMessageBox, QToolBar, QWidget
    except ImportError as error:
        raise SystemExit(
            "PySide6 is required for in-window drag/drop UI. Install with: pip install PySide6"
        ) from error

    return (
        QApplication,
        QMainWindow,
        QWidget,
        QLabel,
        QFileDialog,
        QMessageBox,
        QToolBar,
        QAction,
        Qt,
        QByteArray,
        QRectF,
        QSvgRenderer,
    )


class DropSvgWidget:
    def __init__(self, qt: tuple[object, ...], on_drop: object):
        (
            _QApplication,
            _QMainWindow,
            QWidget,
            QLabel,
            _QFileDialog,
            _QMessageBox,
            _QToolBar,
            _QAction,
            Qt,
            QByteArray,
            QRectF,
            QSvgRenderer,
        ) = qt

        class _Widget(QWidget):
            def __init__(self, on_drop_cb: object) -> None:
                super().__init__()
                self._on_drop_cb = on_drop_cb
                self._renderer = QSvgRenderer(self)
                self._svg_bytes = QByteArray()
                self._title = QLabel("Drop a file or folder here", self)
                self._title.setAlignment(Qt.AlignCenter)
                self._drop_hover = False
                self._zoom = 1.0
                self._min_zoom = 0.2
                self._max_zoom = 6.0
                self._pan_x = 0.0
                self._pan_y = 0.0
                self.setAcceptDrops(True)
                self.setMinimumSize(900, 640)

            def set_svg_text(self, svg_text: str) -> None:
                self._svg_bytes = QByteArray(svg_text.encode("utf-8"))
                loaded = self._renderer.load(self._svg_bytes)
                if not loaded:
                    raise ValueError("Unable to load generated SVG.")
                self._zoom = 1.0
                self._pan_x = 0.0
                self._pan_y = 0.0
                self._title.hide()
                self.update()

            def _scene_size(self) -> tuple[float, float]:
                size = self._renderer.defaultSize()
                if size.isValid() and size.width() > 0 and size.height() > 0:
                    return float(size.width()), float(size.height())
                return float(self.width()), float(self.height())

            def _clamp_zoom(self, value: float) -> float:
                return max(self._min_zoom, min(self._max_zoom, value))

            def paintEvent(self, event) -> None:  # noqa: N802
                super().paintEvent(event)
                from PySide6.QtGui import QColor, QPainter, QPen

                painter = QPainter(self)
                painter.fillRect(self.rect(), QColor("#f3f4f6"))

                if self._renderer.isValid():
                    scene_w, scene_h = self._scene_size()
                    center_x = self.width() / 2.0
                    center_y = self.height() / 2.0

                    painter.save()
                    painter.translate(center_x + self._pan_x, center_y + self._pan_y)
                    painter.scale(self._zoom, self._zoom)
                    painter.translate(-scene_w / 2.0, -scene_h / 2.0)
                    self._renderer.render(painter, QRectF(0.0, 0.0, scene_w, scene_h))
                    painter.restore()

                if self._drop_hover:
                    pen = QPen(QColor("#2563eb"))
                    pen.setWidth(3)
                    pen.setStyle(Qt.DashLine)
                    painter.setPen(pen)
                    painter.setBrush(QColor(37, 99, 235, 26))
                    overlay_rect = self.rect().adjusted(12, 12, -12, -12)
                    painter.drawRoundedRect(overlay_rect, 10, 10)

                painter.end()

            def resizeEvent(self, event) -> None:  # noqa: N802
                super().resizeEvent(event)
                self._title.setGeometry(self.rect())

            def dragEnterEvent(self, event) -> None:  # noqa: N802
                mime = event.mimeData()
                if mime.hasUrls():
                    for url in mime.urls():
                        if url.isLocalFile():
                            self._drop_hover = True
                            self.update()
                            event.acceptProposedAction()
                            return
                event.ignore()

            def dragMoveEvent(self, event) -> None:  # noqa: N802
                mime = event.mimeData()
                if mime.hasUrls():
                    self._drop_hover = True
                    self.update()
                    event.acceptProposedAction()
                    return
                event.ignore()

            def dragLeaveEvent(self, event) -> None:  # noqa: N802
                self._drop_hover = False
                self.update()
                event.accept()

            def dropEvent(self, event) -> None:  # noqa: N802
                self._drop_hover = False
                self.update()
                for url in event.mimeData().urls():
                    if url.isLocalFile():
                        path = Path(url.toLocalFile())
                        self._on_drop_cb(path)
                        event.acceptProposedAction()
                        return
                event.ignore()

            def wheelEvent(self, event) -> None:  # noqa: N802
                angle = event.angleDelta()

                if event.modifiers() & Qt.ControlModifier:
                    if angle.y() == 0:
                        event.accept()
                        return

                    old_zoom = self._zoom
                    factor = 1.15 ** (angle.y() / 120.0)
                    new_zoom = self._clamp_zoom(old_zoom * factor)
                    if new_zoom == old_zoom:
                        event.accept()
                        return

                    cursor = event.position()
                    center_x = self.width() / 2.0
                    center_y = self.height() / 2.0
                    scene_x = (cursor.x() - center_x - self._pan_x) / old_zoom
                    scene_y = (cursor.y() - center_y - self._pan_y) / old_zoom

                    self._zoom = new_zoom
                    self._pan_x = cursor.x() - center_x - (self._zoom * scene_x)
                    self._pan_y = cursor.y() - center_y - (self._zoom * scene_y)
                    self.update()
                    event.accept()
                    return

                pan_step = 40.0
                if event.modifiers() & Qt.ShiftModifier:
                    units = angle.y() / 120.0 if angle.y() != 0 else angle.x() / 120.0
                    self._pan_x += units * pan_step
                else:
                    self._pan_y += (angle.y() / 120.0) * pan_step
                    if angle.x() != 0:
                        self._pan_x += (angle.x() / 120.0) * pan_step

                self.update()
                event.accept()

        self.widget_type = _Widget


def _build_main_window(initial_input: Path | None = None) -> int:
    qt = _import_qt()
    (
        QApplication,
        QMainWindow,
        _QWidget,
        _QLabel,
        QFileDialog,
        QMessageBox,
        QToolBar,
        QAction,
        _Qt,
        _QByteArray,
        _QRectF,
        _QSvgRenderer,
    ) = qt

    app = QApplication(sys.argv)

    class MainWindow(QMainWindow):
        def __init__(self) -> None:
            super().__init__()
            self.setWindowTitle("Ballsack")
            self.resize(1100, 780)
            self._last_input: Path | None = None
            self._last_svg_path: Path | None = None

            drop_factory = DropSvgWidget(qt, self._load_from_path)
            self.viewer = drop_factory.widget_type(self._load_from_path)
            self.setCentralWidget(self.viewer)

            self._install_toolbar()
            self.statusBar().showMessage("Drop xml/json file or a folder.")

            if initial_input is not None:
                self._load_from_path(initial_input)

        def _install_toolbar(self) -> None:
            toolbar = QToolBar("Main", self)
            self.addToolBar(toolbar)

            open_action = QAction("Open...", self)
            open_action.triggered.connect(self._open_dialog)
            toolbar.addAction(open_action)

            open_dir_action = QAction("Open Folder...", self)
            open_dir_action.triggered.connect(self._open_folder_dialog)
            toolbar.addAction(open_dir_action)

            reload_action = QAction("Reload", self)
            reload_action.triggered.connect(self._reload)
            toolbar.addAction(reload_action)

            save_action = QAction("Save SVG As...", self)
            save_action.triggered.connect(self._save_svg_as)
            toolbar.addAction(save_action)

        def _open_dialog(self) -> None:
            file_path, _filt = QFileDialog.getOpenFileName(
                self,
                "Open XML or JSON",
                str(Path.cwd()),
                "Supported (*.xml *.json);;All Files (*)",
            )
            if file_path:
                self._load_from_path(Path(file_path))

        def _open_folder_dialog(self) -> None:
            dir_path = QFileDialog.getExistingDirectory(self, "Open Folder", str(Path.cwd()))
            if dir_path:
                self._load_from_path(Path(dir_path))

        def _reload(self) -> None:
            if self._last_input is None:
                self.statusBar().showMessage("Nothing to reload.")
                return
            self._load_from_path(self._last_input)

        def _save_svg_as(self) -> None:
            if self._last_svg_path is None or not self._last_svg_path.exists():
                self.statusBar().showMessage("No SVG to save yet.")
                return

            save_path, _filt = QFileDialog.getSaveFileName(
                self,
                "Save SVG",
                str(Path.cwd() / "ballsack.svg"),
                "SVG (*.svg)",
            )
            if not save_path:
                return

            Path(save_path).write_text(self._last_svg_path.read_text(encoding="utf-8"), encoding="utf-8")
            self.statusBar().showMessage(f"Saved {save_path}")

        def _load_from_path(self, path: Path) -> None:
            try:
                resolved = path.expanduser().resolve()
                bnfo = load_bnfo_from_path(resolved)
                render_tree = build_render_tree(bnfo)
                svg_text = render_svg(render_tree)

                self.viewer.set_svg_text(svg_text)
                self._last_input = resolved

                tmp_dir = Path(tempfile.gettempdir()) / "ballsack"
                tmp_dir.mkdir(parents=True, exist_ok=True)
                svg_out = tmp_dir / "latest-drop.svg"
                svg_out.write_text(svg_text, encoding="utf-8")
                self._last_svg_path = svg_out

                self.statusBar().showMessage(f"Loaded {resolved}")
            except Exception as error:  # noqa: BLE001
                QMessageBox.critical(self, "Load Failed", str(error))
                self.statusBar().showMessage(f"Load failed: {error}")

    window = MainWindow()
    window.show()
    return app.exec()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Ballsack in-window drag/drop viewer")
    parser.add_argument("--input", type=Path, default=None, help="Optional initial file/folder")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    initial = args.input.expanduser().resolve() if args.input else None
    return _build_main_window(initial)


if __name__ == "__main__":
    raise SystemExit(main())