package net.saoju.dramatown;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import net.saoju.dramatown.Adapters.UserEpfavsAdapter;
import net.saoju.dramatown.Models.EpisodeFavorites;
import net.saoju.dramatown.Utils.ItemDivider;
import net.saoju.dramatown.Utils.LazyFragment;

import java.util.Collections;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.GsonConverterFactory;
import retrofit2.Response;
import retrofit2.Retrofit;

public class UserEpfavsFragment extends LazyFragment {

    SwipeRefreshLayout swipeRefreshLayout;
    private UserEpfavsAdapter adapter;
    private LinearLayoutManager layoutManager;

    SaojuService service;

    private int currentPage;
    private String nextPageUrl;

    private int user;
    private int type;

    public UserEpfavsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_favorites, container, false);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new ItemDivider(getContext(), R.drawable.light_divider));
        adapter = new UserEpfavsAdapter(getActivity(), Collections.EMPTY_LIST);
        recyclerView.setAdapter(adapter);
        Bundle bundle = getArguments();
        user = bundle.getInt("user");
        type = bundle.getInt("type");
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                load();
            }
        });
        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE && adapter != null
                        && adapter.getItemCount() == layoutManager.findLastVisibleItemPosition() + 1) {
                    loadMore();
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });
        isPrepared = true;
        swipeRefreshLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                swipeRefreshLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                load();
            }
        });
        return view;
    }

    @Override
    protected void load() {
        if (!getUserVisibleHint()) {
            return;
        }
        swipeRefreshLayout.setRefreshing(true);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SaojuService.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        service = retrofit.create(SaojuService.class);
        Call<EpisodeFavorites> call = service.getUserEpfavs(String.valueOf(user), String.valueOf(type), null);
        call.enqueue(new Callback<EpisodeFavorites>() {
            @Override
            public void onResponse(Response<EpisodeFavorites> response) {
                EpisodeFavorites favorites = response.body();
                currentPage = favorites.getCurrent_page();
                nextPageUrl = favorites.getNext_page_url();
                adapter.reset(favorites.getData());
                swipeRefreshLayout.setRefreshing(false);
                hasLoadedOnce = true;
            }

            @Override
            public void onFailure(Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void loadMore() {
        if (nextPageUrl == null || nextPageUrl.isEmpty() || swipeRefreshLayout.isRefreshing()) {
            return;
        }
        swipeRefreshLayout.setRefreshing(true);
        Call<EpisodeFavorites> newCall = service.getUserEpfavs(
                String.valueOf(user), String.valueOf(type), String.valueOf(currentPage + 1));
        newCall.enqueue(new Callback<EpisodeFavorites>() {
            @Override
            public void onResponse(Response<EpisodeFavorites> response) {
                EpisodeFavorites favorites = response.body();
                currentPage = favorites.getCurrent_page();
                nextPageUrl = favorites.getNext_page_url();
                adapter.addAll(favorites.getData());
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onFailure(Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }
}
