package gizz.tapes

import platform.UIKit.UIViewController

// The app is Compose-only (no UIKit navigation stack on top), so the controller MainViewController()
// hands to Swift at launch is always the correct place to present native UIKit UI (pickers, etc) from.
object RootViewControllerHolder {
    var current: UIViewController? = null
}
