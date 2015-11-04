package zhou.allen.bruinmenu;

/*
Custom ExpandableListAdapter class
(Mostly) Written by Ravi Tamada in the AndroidHive tutorial at http://www.androidhive.info/2013/07/android-expandable-list-view-tutorial/
10/19/2015
(Slightly) Modified by Preetham Reddy Narayanareddy
*/

import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import zhou.allen.bruinmenu.R;

public class ExpandableListAdapter extends BaseExpandableListAdapter {

    private Context _context;
    private List<String> _listDataHeader; // header titles
    // child data in format of header title, child title
    private HashMap<String, List<MenuItem>> _listDataChild;
    private HashMap<String, List<Boolean>> _listChildrenIsKitchen;

    public ExpandableListAdapter(Context context, List<String> listDataHeader, HashMap<String, List<MenuItem>> listChildData, HashMap<String, List<Boolean>> listChildrenIsKitchen) {
        this._context = context;
        this._listDataHeader = listDataHeader;
        this._listDataChild = listChildData;
        this._listChildrenIsKitchen = listChildrenIsKitchen;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return this._listDataChild.get(this._listDataHeader.get(groupPosition)).get(childPosition).getName();
    }

    public Boolean getChildIsKitchen(int groupPosition, int childPosition) {
        return this._listChildrenIsKitchen.get(this._listDataHeader.get(groupPosition)).get(childPosition);
    }
    public Boolean getChildIsVeg(int groupPosition, int childPosition) {
        return this._listDataChild.get(this._listDataHeader.get(groupPosition)).get(childPosition).isVegetarian();
    }
    public Boolean getChildIsFav(int groupPosition, int childPosition) {
        return this._listDataChild.get(this._listDataHeader.get(groupPosition)).get(childPosition).isFavorite();
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        final String childText = (String) getChild(groupPosition, childPosition);

        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this._context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if(getChildIsKitchen(groupPosition, childPosition)) convertView = infalInflater.inflate(R.layout.list_item, null);
            //if(childPosition == 0) convertView = infalInflater.inflate(R.layout.list_kitchen, null);
            else {
                convertView = infalInflater.inflate(R.layout.list_item, null);
                if(getChildIsVeg(groupPosition, childPosition));///TODO: get toggleabel icon here and toggle it
                if(getChildIsFav(groupPosition, childPosition)); //TODO: toggle the star button
            }
        }

        TextView txtListChild = (TextView) convertView.findViewById(R.id.lblListItem);
        txtListChild.setText(childText);
        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this._listDataChild.get(this._listDataHeader.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return this._listDataHeader.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return this._listDataHeader.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        String headerTitle = (String) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this._context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.list_group, null);
        }

        TextView lblListHeader = (TextView) convertView.findViewById(R.id.lblListHeader);
        lblListHeader.setTypeface(null, Typeface.BOLD);
        lblListHeader.setText(headerTitle);

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}