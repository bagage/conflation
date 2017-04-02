// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.conflation.config;

import javax.swing.JPanel;

import org.openstreetmap.josm.data.Preferences;

import com.vividsolutions.jcs.conflate.polygonmatch.FCMatchFinder;

public abstract class MatchFinderPanel extends JPanel {

    public abstract FCMatchFinder getMatchFinder();

    public abstract void savePreferences(Preferences pref);

}
