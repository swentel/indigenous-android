package com.indieweb.indigenous.micropub.source;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.indieweb.indigenous.R;
import com.indieweb.indigenous.model.PostListItem;
import com.indieweb.indigenous.model.User;
import com.indieweb.indigenous.util.Accounts;
import com.indieweb.indigenous.util.Preferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostListActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    private PostListAdapter adapter;
    private List<PostListItem> PostListItems = new ArrayList<>();
    SwipeRefreshLayout refreshLayout;
    ListView listView;
    User user;
    Button loadMoreButton;
    boolean loadMoreButtonAdded = false;
    String[] olderItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_source_post_list);

        listView = findViewById(R.id.source_post_list);
        refreshLayout = findViewById(R.id.refreshSourcePostList);
        refreshLayout.setOnRefreshListener(this);
        loadMoreButton = new Button(this);
        loadMoreButton.setText(R.string.load_more);
        loadMoreButton.setTextColor(getResources().getColor(R.color.textColor));
        loadMoreButton.setBackgroundColor(getResources().getColor(R.color.loadMoreButtonBackgroundColor));
        user = new Accounts(this).getCurrentUser();
        startPostList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.source_post_list_top_menu, menu);

        String postTypes = user.getPostTypes();
        if (postTypes == null || postTypes.length() == 0) {
            // TODO make button names consistent (everywhere)
            // TODO check names of all id's and check the 'standards' and document that.
            MenuItem itemDelete = menu.findItem(R.id.source_post_list_filter);
            itemDelete.setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.source_post_list_refresh:
                refreshLayout.setRefreshing(true);
                startPostList();
                return true;
            case R.id.source_post_list_filter:
                Intent PostListFilter = new Intent(getBaseContext(), PostListFilterActivity.class);
                startActivityForResult(PostListFilter, 1);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        startPostList();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                boolean refresh = data.getBooleanExtra("refresh", false);
                if (refresh) {
                    refreshLayout.setRefreshing(true);
                    startPostList();
                }
            }
        }
    }

    /**
     * Checks the state of the pull to refresh.
     */
    public void checkRefreshingStatus() {
        if (refreshLayout.isRefreshing()) {
            Toast.makeText(getApplicationContext(), getString(R.string.source_post_list_items_refreshed), Toast.LENGTH_SHORT).show();
            refreshLayout.setRefreshing(false);
        }
    }

    /**
     * Start with post list.
     */
    public void startPostList() {
        boolean deleteEnabled = Preferences.getPreference(this, "pref_key_experimental_delete", false);
        PostListItems = new ArrayList<>();
        adapter = new PostListAdapter(this, PostListItems, user, deleteEnabled);
        listView.setAdapter(adapter);
        getSourcePostListItems("");
    }

    /**
     * Get items in channel.
     */
    public void getSourcePostListItems(String pagerAfter) {

        String MicropubEndpoint = user.getMicropubEndpoint();
        // Some endpoints already contain GET params. Instead of overriding the getParams method, we
        // just check it here.
        if (MicropubEndpoint.contains("?")) {
            MicropubEndpoint += "&q=source";
        }
        else {
            MicropubEndpoint += "?q=source";
        }

        if (pagerAfter.length() > 0) {
            MicropubEndpoint += "&after=" + pagerAfter;
        }
        olderItems = new String[1];

        // Filter on post type.
        String postType = Preferences.getPreference(getApplicationContext(), "source_post_list_filter_post_type", "all_source_post_types");
        if (!postType.equals("all_source_post_types")) {
            MicropubEndpoint += "&post-type=" + postType;
        }

        // Limit.
        String limit = Preferences.getPreference(getApplicationContext(), "source_post_list_filter_post_limit", "10");
        MicropubEndpoint += "&limit=" + limit;

        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
        StringRequest getRequest = new StringRequest(Request.Method.GET, MicropubEndpoint,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        try {
                            JSONObject object;
                            JSONObject micropubResponse = new JSONObject(response);
                            JSONArray itemList = micropubResponse.getJSONArray("items");

                            // Paging. Can be empty.
                            if (micropubResponse.has("paging")) {
                                try {
                                    if (micropubResponse.getJSONObject("paging").has("after")) {
                                        olderItems[0] = micropubResponse.getJSONObject("paging").getString("after");
                                    }
                                }
                                catch (JSONException ignored) {}
                            }

                            for (int i = 0; i < itemList.length(); i++) {
                                object = itemList.getJSONObject(i).getJSONObject("properties");
                                PostListItem item = new PostListItem();

                                String url = "";
                                String name = "";
                                String content = "";
                                String published = "";

                                // url.
                                if (object.has("url")) {
                                    url = object.getJSONArray("url").get(0).toString();
                                }
                                item.setUrl(url);

                                // published.
                                if (object.has("published")) {
                                    published = object.getJSONArray("published").get(0).toString();
                                }
                                item.setPublished(published);

                                // content.
                                if (object.has("content")) {
                                    content = object.getJSONArray("content").get(0).toString();
                                }
                                item.setContent(content);

                                // name.
                                if (object.has("name")) {
                                    name = object.getJSONArray("name").get(0).toString();
                                }
                                item.setName(name);

                                PostListItems.add(item);
                            }

                            adapter.notifyDataSetChanged();

                            if (olderItems[0] != null && olderItems[0].length() > 0) {

                                if (!loadMoreButtonAdded) {
                                    loadMoreButtonAdded = true;
                                    listView.addFooterView(loadMoreButton);
                                }

                                // TODO check this warning
                                loadMoreButton.setOnTouchListener(loadMoreTouch);
                            }
                            else {
                                if (loadMoreButtonAdded) {
                                    listView.removeFooterView(loadMoreButton);
                                }
                            }

                        }
                        catch (JSONException e) {
                            Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }

                        checkRefreshingStatus();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(), getString(R.string.no_posts_found), Toast.LENGTH_SHORT).show();
                        checkRefreshingStatus();
                    }
                }
        )
        {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Accept", "application/json");
                headers.put("Authorization", "Bearer " + user.getAccessToken());
                return headers;
            }

        };

        getRequest.setRetryPolicy(new DefaultRetryPolicy(0, -1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(getRequest);
    }

    /**
     * Load more touch button.
     */
    private View.OnTouchListener loadMoreTouch = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent motionEvent) {
            switch(motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    int downColorTouch = getResources().getColor(R.color.loadMoreButtonBackgroundColorTouched);
                    loadMoreButton.setBackgroundColor(downColorTouch);
                    break;
                case MotionEvent.ACTION_CANCEL:
                    loadMoreButton.setBackgroundColor(getResources().getColor(R.color.loadMoreButtonBackgroundColor));
                    break;
                case MotionEvent.ACTION_UP:
                    int downColor = getResources().getColor(R.color.loadMoreButtonBackgroundColor);
                    loadMoreButton.setBackgroundColor(downColor);
                    getSourcePostListItems(olderItems[0]);
                    break;

            }
            return true;
        }
    };

}
