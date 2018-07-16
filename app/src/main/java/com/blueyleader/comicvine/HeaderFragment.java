package com.blueyleader.comicvine;

import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class HeaderFragment extends ListFragment {

    String[] headers =  new String[] {"General", "To Rip", "Backup", "Export"};
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View view =inflater.inflate(R.layout.header_fragment, viewGroup, false);
        ArrayAdapter adapter = new ArrayAdapter(getActivity(),
                android.R.layout.simple_list_item_activated_1, headers);
        setListAdapter(adapter);
        return view;
    }

    public void onListItemClick(ListView listView, View view, int position, long id) {
        SettingsFragment txt = (SettingsFragment)getFragmentManager().findFragmentById(R.id.settings);
        //txt.display[0]=position+"";
        //txt.setDisplay();
        //txt.display(word[position]);
        //getListView().setSelector(android.R.color.holo_red_dark);
    }
}