import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        IOSKoinInitializer().initialize(
            nativeDependencyProvider: nil
        )
    }


    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    AppDependencies().provideDeeplinkHandler().handle(urlString: url.absoluteString)
                }
        }
    }
}
