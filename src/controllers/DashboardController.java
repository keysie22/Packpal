/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controllers;

import views.DashboardView;
import views.WelcomeView;

public class DashboardController {
    private DashboardView view;

    public DashboardController(DashboardView view) {
        this.view = view;
    }

   public void handleBackToWelcome() {
    view.navigateToWelcome();
}

public void handleLogout() {
    view.setVisible(false);
    WelcomeView welcomeView = new WelcomeView();
    welcomeView.setVisible(true);
}
    // Add methods for tab actions if needed, e.g., handleEditList(String listName)
}
