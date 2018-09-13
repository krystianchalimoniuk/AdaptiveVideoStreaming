package adaptstreamingvideo.com.adaptstreamingvideo;

/**
 * Created by Krystiano on 2018-03-15.
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;


import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
public class Settings extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Note that none of the preferences are actually defined here.
        // They're all in the XML file res/xml/preferences.xml.
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();

    }

    public static class MyPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);


            final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
            final Preference videoEnabled = findPreference("stream_video");
            final Preference audioEnabled = findPreference("stream_audio");
            final ListPreference audioEncoder = (ListPreference) findPreference("audio_encoder");
            final ListPreference videoEncoder = (ListPreference) findPreference("video_encoder");
            final ListPreference videoResolution = (ListPreference) findPreference("video_resolution");
            final ListPreference videoBitrate = (ListPreference) findPreference("video_bitrate");
            final ListPreference videoFramerate = (ListPreference) findPreference("video_framerate");

            boolean videoState = settings.getBoolean("stream_video", true);
            videoEncoder.setEnabled(videoState);
            videoResolution.setEnabled(videoState);
            videoBitrate.setEnabled(videoState);
            videoFramerate.setEnabled(videoState);



            videoResolution.setSummary(getString(R.string.settings0)+" "+videoResolution.getValue()+"px");
            videoFramerate.setSummary(getString(R.string.settings1)+" "+videoFramerate.getValue()+"fps");
            videoBitrate.setSummary(getString(R.string.settings2)+" "+videoBitrate.getValue()+"kbps");

            audioEncoder.setEnabled(settings.getBoolean("stream_audio", false));



            videoResolution.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    SharedPreferences.Editor editor = settings.edit();
                    Pattern pattern = Pattern.compile("([0-9]+)x([0-9]+)");
                    Matcher matcher = pattern.matcher((String)newValue);
                    matcher.find();
                    editor.putInt("video_resX", Integer.parseInt(matcher.group(1)));
                    editor.putInt("video_resY", Integer.parseInt(matcher.group(2)));
                    editor.commit();
                    videoResolution.setSummary(getString(R.string.settings0)+" "+(String)newValue+"px");
                    return true;
                }
            });

            videoFramerate.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    videoFramerate.setSummary(getString(R.string.settings1)+" "+(String)newValue+"fps");
                    return true;
                }
            });

            videoBitrate.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    videoBitrate.setSummary(getString(R.string.settings2)+" "+(String)newValue+"kbps");
                    return true;
                }
            });

            videoEnabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean state = (Boolean)newValue;
                    videoEncoder.setEnabled(state);
                    videoResolution.setEnabled(state);
                    videoBitrate.setEnabled(state);
                    videoFramerate.setEnabled(state);
                    return true;
                }
            });

            audioEnabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean state = (Boolean)newValue;
                    audioEncoder.setEnabled(state);
                    return true;
                }
            });

        }
    }
}