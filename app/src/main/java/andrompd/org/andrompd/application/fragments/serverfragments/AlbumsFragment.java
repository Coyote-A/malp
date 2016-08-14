/*
 * Copyright (C) 2016  Hendrik Borghorst
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package andrompd.org.andrompd.application.fragments.serverfragments;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import java.util.List;

import andrompd.org.andrompd.R;
import andrompd.org.andrompd.application.adapters.AlbumsGridAdapter;
import andrompd.org.andrompd.application.callbacks.FABFragmentCallback;
import andrompd.org.andrompd.application.loaders.AlbumsLoader;
import andrompd.org.andrompd.application.utils.ScrollSpeedListener;
import andrompd.org.andrompd.application.utils.ThemeUtils;
import andrompd.org.andrompd.mpdservice.handlers.serverhandler.MPDQueryHandler;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;

public class AlbumsFragment extends GenericMPDFragment<List<MPDAlbum>> implements AdapterView.OnItemClickListener {
    public final static String TAG = AlbumsFragment.class.getSimpleName();

    /**
     * Definition of bundled extras
     */
    public static final String BUNDLE_STRING_EXTRA_ARTISTNAME = "artistname";

    /**
     * GridView adapter object used for this GridView
     */
    private AlbumsGridAdapter mAlbumsAdapter;

    /**
     * Save the root GridView for later usage.
     */
    private GridView mRootGrid;

    /**
     * Save the last position here. Gets reused when the user returns to this view after selecting sme
     * albums.
     */
    private int mLastPosition;

    /**
     * Save the swipe layout for later usage
     */
    private SwipeRefreshLayout mSwipeRefreshLayout;


    private String mArtistName;

    private AlbumSelectedCallback mAlbumSelectCallback;

    private FABFragmentCallback mFABCallback = null;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_gridview, container, false);

        // get gridview
        mRootGrid = (GridView) rootView.findViewById(R.id.grid_refresh_gridview);

        mAlbumsAdapter = new AlbumsGridAdapter(getActivity(), mRootGrid);

        /* Check if an artistname was given in the extras */
        Bundle args = getArguments();
        if (null != args) {
            mArtistName = args.getString(BUNDLE_STRING_EXTRA_ARTISTNAME);
        } else {
            mArtistName = "";
        }

        mRootGrid.setAdapter(mAlbumsAdapter);
        mRootGrid.setOnScrollListener(new ScrollSpeedListener(mAlbumsAdapter, mRootGrid));
        mRootGrid.setOnItemClickListener(this);

        // register for context menu
        registerForContextMenu(mRootGrid);


        setHasOptionsMenu(true);

        // get swipe layout
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.refresh_layout);
        // set swipe colors
        mSwipeRefreshLayout.setColorSchemeColors(ThemeUtils.getThemeColor(getContext(), R.attr.colorAccent),
                ThemeUtils.getThemeColor(getContext(), R.attr.colorPrimary));
        // set swipe refresh listener
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                refreshContent();
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        if ( null != mFABCallback  ) {
            if ( null != mArtistName && !mArtistName.equals("")) {
                mFABCallback.setupFAB(true, new FABOnClickListener());
                mFABCallback.setupToolbar(mArtistName, true, false, false);

            } else {
                mFABCallback.setupFAB(false, null);
                mFABCallback.setupToolbar(getString(R.string.app_name), true, true, false);

            }
        }
    }

    /**
     * Called when the fragment is first attached to its context.
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mAlbumSelectCallback = (AlbumSelectedCallback) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnArtistSelectedListener");
        }


        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mFABCallback = (FABFragmentCallback) context;
        } catch (ClassCastException e) {
            mFABCallback = null;
        }
    }


    /**
     * Create the context menu.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_album, menu);
    }

    /**
     * Hook called when an menu item in the context menu is selected.
     * @param item The menu item that was selected.
     * @return True if the hook was consumed here.
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
            case R.id.fragment_albums_action_enqueue:
                enqueueAlbum(info.position);
                return true;
            case R.id.fragment_albums_action_play:
                playAlbum(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * Initialize the options menu.
     * Be sure to call {@link #setHasOptionsMenu} before.
     *
     * @param menu         The container for the custom options menu.
     * @param menuInflater The inflater to instantiate the layout.
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if ( null != mArtistName && !mArtistName.equals("")) {
            menuInflater.inflate(R.menu.fragment_menu_albums, menu);

            // get tint color
            int tintColor = ThemeUtils.getThemeColor(getContext(), android.R.attr.textColor);

            Drawable drawable = menu.findItem(R.id.action_add_artist).getIcon();
            drawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTint(drawable, tintColor);
            menu.findItem(R.id.action_add_artist).setIcon(drawable);
        }
        super.onCreateOptionsMenu(menu, menuInflater);
    }

    /**
     * Hook called when an menu item in the options menu is selected.
     *
     * @param item The menu item that was selected.
     * @return True if the hook was consumed here.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_artist:
                enqueueArtist();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * This method creates a new loader for this fragment.
     *
     * @param id
     * @param args
     * @return
     */
    @Override
    public Loader<List<MPDAlbum>> onCreateLoader(int id, Bundle args) {
        return new AlbumsLoader(getActivity(), mArtistName);
    }

    /**
     * Called when the loader finished loading its data.
     *
     * @param loader The used loader itself
     * @param data   Data of the loader
     */
    @Override
    public void onLoadFinished(Loader<List<MPDAlbum>> loader, List<MPDAlbum> data) {
        // Set the actual data to the adapter.
        mAlbumsAdapter.swapModel(data);

        // Reset old scroll position
        if (mLastPosition >= 0) {
            mRootGrid.setSelection(mLastPosition);
            mLastPosition = -1;
        }

        // change refresh state
        mSwipeRefreshLayout.setRefreshing(false);
    }

    /**
     * If a loader is reset the model data should be cleared.
     *
     * @param loader Loader that was resetted.
     */
    @Override
    public void onLoaderReset(Loader<List<MPDAlbum>> loader) {
        // Clear the model data of the adapter.
        mAlbumsAdapter.swapModel(null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mLastPosition = position;

        MPDAlbum album = (MPDAlbum) mAlbumsAdapter.getItem(position);

        mAlbumSelectCallback.onAlbumSelected(album.getName(), mArtistName);
    }



    public interface AlbumSelectedCallback {
        void onAlbumSelected(String albumname, String artistname);
    }


    private void enqueueAlbum(int index) {
        MPDAlbum album = (MPDAlbum)mAlbumsAdapter.getItem(index);

        MPDQueryHandler.addArtistAlbum(album.getName(), mArtistName);
    }

    private void playAlbum(int index) {
        MPDAlbum album = (MPDAlbum)mAlbumsAdapter.getItem(index);

        MPDQueryHandler.playArtistAlbum(album.getName(), mArtistName);
    }

    private void enqueueArtist() {
        MPDQueryHandler.addArtist(mArtistName);
    }

    private class FABOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            MPDQueryHandler.playArtist(mArtistName);
        }
    }

}