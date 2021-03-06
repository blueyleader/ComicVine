package com.blueyleader.comiccollector;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import static android.content.Context.MODE_PRIVATE;
import static com.blueyleader.comiccollector.MainActivity.getJson;
import static com.blueyleader.comiccollector.MainActivity.json_id;
import static com.blueyleader.comiccollector.MainActivity.json_name;
import static com.blueyleader.comiccollector.MainActivity.json_results;
import static com.blueyleader.comiccollector.MainActivity.web_api_key;
import static com.blueyleader.comiccollector.MainActivity.web_base;
import static com.blueyleader.comiccollector.MainActivity.web_character;
import static com.blueyleader.comiccollector.MainActivity.web_character_ref;
import static com.blueyleader.comiccollector.MainActivity.web_format;
import static com.blueyleader.comiccollector.MainActivity.web_issue;
import static com.blueyleader.comiccollector.MainActivity.web_issue_ref;
import static com.blueyleader.comiccollector.MainActivity.web_volume;
import static com.blueyleader.comiccollector.MainActivity.web_volume_ref;

public class CollectionFragment extends ListFragment {

    SettingsAdapter adapter;
    boolean firstData = true;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        Log.d("ComicVine","in onCreateView");
        View view =inflater.inflate(R.layout.list_fragment, viewGroup, false);
        adapter = new SettingsAdapter(getActivity().getBaseContext());
        setListAdapter(adapter);
        return view;
    }

    private class SettingsAdapter extends BaseAdapter {
        HashMap<Integer,RipObject> charactersMap = null;
        HashMap<Integer,RipObject> volumesMap = null;
        HashMap<Integer,RipObject> issuesMap = null;
        Context context;

        RipObject[][] set;

        public SettingsAdapter(Context context){
            this.context=context;
            set = new RipObject[3][];
            File file = new File(context.getDir("data", MODE_PRIVATE), "map_characters");
            if(file.exists()){
                try {
                    ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
                    charactersMap = (HashMap<Integer,RipObject>)ois.readObject();
                    ois.close();
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }

            if(charactersMap == null){
                charactersMap = new HashMap<>();
            }

            file = new File(context.getDir("data", MODE_PRIVATE), "map_volumes");
            if(file.exists()){
                try {
                    ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
                    volumesMap = (HashMap<Integer,RipObject>)ois.readObject();
                    ois.close();
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }

            if(volumesMap == null){
                volumesMap = new HashMap<>();
            }

            file = new File(context.getDir("data", MODE_PRIVATE), "map_issues");
            if(file.exists()){
                try {
                    ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
                    issuesMap = (HashMap<Integer,RipObject>)ois.readObject();
                    ois.close();
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }

            if(issuesMap == null){
                issuesMap = new HashMap<>();
            }

            updateData();
        }

        public void updateData() {
            SettingsActivity.self.loading();
            try {
                File file = new File(context.getDir("data", MODE_PRIVATE), "map_characters");
                ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file));
                outputStream.writeObject(charactersMap);
                outputStream.flush();
                outputStream.close();
            }
            catch(Exception e){
                e.printStackTrace();
            }

            try {
                File file = new File(context.getDir("data", MODE_PRIVATE), "map_volumes");
                ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file));
                outputStream.writeObject(volumesMap);
                outputStream.flush();
                outputStream.close();
            }
            catch(Exception e){
                e.printStackTrace();
            }

            try {
                File file = new File(context.getDir("data", MODE_PRIVATE), "map_issues");
                ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file));
                outputStream.writeObject(issuesMap);
                outputStream.flush();
                outputStream.close();
            }
            catch(Exception e){
                e.printStackTrace();
            }

            set[0] = charactersMap.values().toArray(new RipObject[0]);
            Arrays.sort(set[0], new RipObjectCompare());

            set[1] = volumesMap.values().toArray(new RipObject[0]);
            Arrays.sort(set[1], new RipObjectCompare());

            set[2] = issuesMap.values().toArray(new RipObject[0]);
            Arrays.sort(set[2], new RipObjectCompare());

            notifyDataSetChanged();
            SharedPreferences sh = PreferenceManager.getDefaultSharedPreferences (context);
            Boolean update = sh.getBoolean("auto_pull_update",false);
            if(update && !firstData) {
                new UpdateData().execute();
            }
            else{
                firstData = false;
                MainActivity.self.loadingDialog.cancel();
            }
        }

        @Override
        public int getCount() {
            return set.length;
        }

        @Override
        public Object getItem(int position) {
            return set[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup viewGroup) {
            final ViewHolder holder;
            if(convertView == null) {
                holder = new ViewHolder();
                convertView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.preference_category_button, viewGroup, false);
                holder.nameText = convertView.findViewById(R.id.category_title);
                holder.comicList = convertView.findViewById(R.id.list);
                holder.ref = position;
                holder.addButton = convertView.findViewById(R.id.add_button);

                holder.addButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        // 1. Instantiate an AlertDialog.Builder with its constructor
                        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());

                        // 2. Chain together various setter methods to set the dialog characteristics
                        builder.setTitle("please add").setMessage("Enter id of object to collect");

                        builder.setView(R.layout.add_rip_dialog);
                        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                SettingsActivity.self.loading();

                                // User clicked OK button
                                String text =((EditText) ((AlertDialog) dialog).getCurrentFocus().findViewById(R.id.id_edit)).getText().toString();
                                int num = 0;
                                if(text.length()==0){
                                    return;
                                }
                                try {
                                    num = Integer.parseInt(text);
                                }
                                catch(NumberFormatException e){

                                }
                                Log.d("ComicVine","Text was " + num);
                                SharedPreferences sh = PreferenceManager.getDefaultSharedPreferences (context);
                                String key = sh.getString("API_KEY","");
                                if(key.equals("")){
                                    Log.d("ComicVine","no Key");
                                    Toast.makeText(context,"Please add a ComicVine API key before adding objects",Toast.LENGTH_LONG).show();
                                    MainActivity.self.loadingDialog.cancel();
                                    return;
                                }

                                String url ="";
                                switch(position) {
                                    case 0:
                                        url = web_base + web_character + web_character_ref + num + web_api_key + key + web_format;
                                        break;
                                    case 1:
                                        url = web_base + web_volume + web_volume_ref + num + web_api_key + key + web_format;
                                        break;
                                    case 2:
                                        url = web_base + web_issue + web_issue_ref + num + web_api_key + key + web_format;
                                        break;
                                }

                                Log.d("ComicVine","url is: " + url);
                                new JsonGetter().execute(new Object[]{url,position});



                            }
                        });
                        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                            }
                        });

                        // 3. Get the AlertDialog from create()
                        AlertDialog dialog = builder.create();

                        dialog.show();
                    }
                });
                convertView.setTag(holder);
            }
            else{
                holder = (ViewHolder) convertView.getTag();
            }

            switch(position){
                case 0:
                    holder.nameText.setText(R.string.characters);
                    break;
                case 1:
                    holder.nameText.setText(R.string.volumes);
                    break;
                case 2:
                    holder.nameText.setText(R.string.issues);
                    break;

            }
            holder.comicList.removeAllViews();
            for(int x=0;x<set[position].length;x++){
                View child = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.rip_preference, viewGroup, false);
                TextView name = child.findViewById(R.id.rip_name);
                ImageButton remove =  child.findViewById(R.id.rip_remove);
                remove.setTag(set[position][x]);
                remove.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        RipObject rp = (RipObject) v.getTag();

                        switch(rp.type){
                            case 0:
                                charactersMap.remove(Integer.parseInt(rp.id));
                                break;
                            case 1:
                                volumesMap.remove(Integer.parseInt(rp.id));
                                break;
                            case 2:
                                issuesMap.remove(Integer.parseInt(rp.id));
                                break;
                        }
                        updateData();

                    }
                });
                name.setText(set[position][x].name);
                holder.comicList.addView(child);
            }

            return convertView;
        }


        private class RipObjectCompare implements Comparator<RipObject> {

            public int compare(RipObject o1, RipObject o2) {
                // Intentional: Reverse order for this demo
                int name = o1.name.compareTo(o2.name);

                if(name == 0){
                    int date = o1.date.compareTo(o2.date);
                    if(date==0){
                        return o1.id.compareTo(o2.id);
                    }
                    return date;
                }
                return name;
            }
        }

        public class ViewHolder {
            TextView nameText;
            LinearLayout comicList;
            ImageButton addButton;
            int ref;
            int type;
        }

        private class JsonGetter extends AsyncTask<Object,String,Integer>{

            @Override
            protected Integer doInBackground(Object... params) {
                String ret = getJson((String)params[0],false);

                if(ret==null){
                    return -1;
                }
                try {
                    JSONObject base = new JSONObject(ret);
                    int status = base.getInt("status_code");
                    if(status!=1){
                        Log.d("ComicCollector","Error while getting json. status: " + status);
                        MainActivity.self.loadingDialog.cancel();
                        return status;
                    }
                    JSONObject root = base.getJSONObject(json_results);
                    String name = root.getString(json_name);
                    int objId = root.getInt(json_id);
                    RipObject rp = new RipObject(name, objId + "",  "", (int)params[1]);
                    switch(rp.type) {
                        case 0:
                            charactersMap.put(objId, rp);
                            break;
                        case 1:
                            volumesMap.put(objId, rp);
                            break;
                        case 2:
                            issuesMap.put(objId, rp);
                            break;
                    }
                }
                catch(Exception e){
                    e.printStackTrace();
                    return -99;
                }

                return 0;
            }

            protected void onPostExecute(Integer result) {
/*
                1:OK
                100:Invalid API Key
                101:Object Not Found
                102:Error in URL Format
                103:'jsonp' format requires a 'json_callback' argument
                104:Filter Error
                105:Subscriber only video is for subscribers only
*/
                switch(result){
                    case -1:
                        Toast.makeText(context,"Error while getting json from the web",Toast.LENGTH_LONG).show();
                        break;
                    case 100:
                        Toast.makeText(context,"Current API key is invalid",Toast.LENGTH_LONG).show();
                        break;
                    case 101:
                        Toast.makeText(context,"Id of object was not found",Toast.LENGTH_LONG).show();
                        break;
                    case -99:
                        Toast.makeText(context,"Unknown error while getting information. josn stats is: " + result, Toast.LENGTH_LONG).show();
                        break;
                    default:
                        updateData();
                }

                if(result<0){
                    MainActivity.self.loadingDialog.cancel();
                }
            }
        }
    }
}
