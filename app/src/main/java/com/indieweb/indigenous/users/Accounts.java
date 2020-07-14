package com.indieweb.indigenous.users;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.appcompat.app.AlertDialog;

import android.os.Handler;
import android.widget.RelativeLayout;

import com.google.android.material.snackbar.Snackbar;
import com.indieweb.indigenous.LaunchActivity;
import com.indieweb.indigenous.R;
import com.indieweb.indigenous.model.User;
import com.indieweb.indigenous.util.Preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;
import static com.indieweb.indigenous.users.AuthActivity.INDIEWEB_ACCOUNT_TYPE;
import static com.indieweb.indigenous.users.AuthActivity.PIXELFED_ACCOUNT_TYPE;

public class Accounts {

    private final Context context;

    public Accounts(Context context) {
        this.context = context;
    }

    /**
     * Gets the default user.
     *
     * @return User
     */
    public User getDefaultUser() {
        User user = new User();
        boolean foundUser = false;

        SharedPreferences preferences = context.getSharedPreferences("indigenous", MODE_PRIVATE);
        String accountName = preferences.getString("account", "");

        List<User> users = getAllUsers();
        for (User u : users) {
            if (u.getAccount().name.equals(accountName)) {
                user = u;
                foundUser = true;
                user.setValid(true);
            }
        }

        // Anonymous user.
        if (!foundUser) {
            user.setValid(true);
            user.setAnonymous(true);
            user.setMe(context.getString(R.string.anonymous_me));
            user.setName(context.getString(R.string.anonymous));
            user.setAccessToken(Preferences.getPreference(context, "anonymous_token", ""));
            user.setMicrosubEndpoint(Preferences.getPreference(context, "anonymous_microsub_endpoint", context.getString(R.string.anonymous_microsub_endpoint)));
            user.setMicropubEndpoint(Preferences.getPreference(context, "anonymous_micropub_endpoint", ""));
        }

        return user;
    }

    /**
     * Get number of accounts.
     *
     * @return int
     */
    public int getCount() {
        Account[] AllAccounts = this.getAllAccounts();
        return AllAccounts.length;
    }

    /**
     * Switch account dialog.
     *
     * @param activity
     *   The current activity
     * @param user
     *   The user to switch to.
     * @param layout
     *   The current layout.
     */
    public void switchAccount(final Activity activity, final User user, final RelativeLayout layout) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(String.format(context.getString(R.string.account_switch), user.getMe()));
        builder.setPositiveButton(context.getString(R.string.switch_account),new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                Snackbar.make(layout, String.format(context.getString(R.string.account_selected), user.getMe()), Snackbar.LENGTH_SHORT).show();
                SharedPreferences.Editor editor = context.getSharedPreferences("indigenous", MODE_PRIVATE).edit();
                editor.putString("account", user.getAccount().name);
                editor.apply();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent Main = new Intent(context, LaunchActivity.class);
                        context.startActivity(Main);
                        activity.finish();
                    }
                }, 700);

            }
        });
        builder.setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    /**
     * Select account dialog.
     *
     * @param activity
     *   The current activity
     * @param layout
     *   The current layout
     */
    public void selectAccount(final Activity activity, final RelativeLayout layout) {
        final List<String> accounts = new ArrayList<>();

        final Account[] AllAccounts = this.getAllAccounts();
        for (Account account: AllAccounts) {
            accounts.add(account.name);
        }

        final CharSequence[] accountItems = accounts.toArray(new CharSequence[0]);
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(activity.getString(R.string.account_select));

        builder.setCancelable(true);
        builder.setItems(accountItems, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index) {
                Snackbar.make(layout, String.format(context.getString(R.string.account_selected), accounts.get(index)), Snackbar.LENGTH_SHORT).show();
                SharedPreferences.Editor editor = context.getSharedPreferences("indigenous", MODE_PRIVATE).edit();
                editor.putString("account", accounts.get(index));
                editor.apply();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent Main = new Intent(context, LaunchActivity.class);
                        context.startActivity(Main);
                        activity.finish();
                    }
                }, 700);
            }
        });
        builder.show();
    }

    /**
     * Returns all accounts.
     *
     * @return Account[]
     */
    public Account[] getAllAccounts() {
        AccountManager accountManager = AccountManager.get(context);
        ArrayList<Account> list = new ArrayList<>(Arrays.asList(accountManager.getAccountsByType(INDIEWEB_ACCOUNT_TYPE)));
        Account[] pixelFedAccounts = accountManager.getAccountsByType(PIXELFED_ACCOUNT_TYPE);
        list.addAll(Arrays.asList(pixelFedAccounts));
        return list.toArray(new Account[0]);
    }

    /**
     * Returns all users.
     *
     * @return User[]
     */
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        AccountManager accountManager = AccountManager.get(context);
        for (Account a : accountManager.getAccountsByType(INDIEWEB_ACCOUNT_TYPE)) {
            User user = new User();
            user.setAccount(a);
            user.setMe(a.name);
            String token = "";
            try {
                token = accountManager.peekAuthToken(a, AuthActivity.INDIEWEB_TOKEN_TYPE);
            }
            catch (Exception ignored) {}

            user.setAccessToken(token);
            user.setAccountType(INDIEWEB_ACCOUNT_TYPE);
            user.setExternalId(accountManager.getUserData(a, "external_id"));
            user.setAvatar(accountManager.getUserData(a, "author_avatar"));
            user.setName(accountManager.getUserData(a, "author_name"));
            user.setTokenEndpoint(accountManager.getUserData(a, "token_endpoint"));
            user.setAuthorizationEndpoint(accountManager.getUserData(a, "authorization_endpoint"));
            user.setMicrosubEndpoint(accountManager.getUserData(a, "microsub_endpoint"));
            user.setMicropubEndpoint(accountManager.getUserData(a, "micropub_endpoint"));
            user.setMicropubMediaEndpoint(accountManager.getUserData(a, "micropub_media_endpoint"));
            user.setSyndicationTargets(accountManager.getUserData(a, "syndication_targets"));
            user.setPostTypes(accountManager.getUserData(a, "post_types"));
            user.setAccount(a);
            users.add(user);
        }

        for (Account a : accountManager.getAccountsByType(PIXELFED_ACCOUNT_TYPE)) {
            User user = new User();
            user.setMe(a.name);
            String token = "";
            try {
                token = accountManager.peekAuthToken(a, AuthActivity.PIXELFED_TOKEN_TYPE);
            }
            catch (Exception ignored) {}

            user.setAccessToken(token);
            user.setAvatar(accountManager.getUserData(a, "author_avatar"));
            user.setName(accountManager.getUserData(a, "author_name"));
            user.setExternalId(accountManager.getUserData(a, "external_id"));
            user.setClientId(accountManager.getUserData(a, "client_id"));
            user.setClientSecret(accountManager.getUserData(a, "client_secret"));
            user.setAccountType(PIXELFED_ACCOUNT_TYPE);
            user.setAccount(a);
            users.add(user);
        }

        return users;
    }

    /**
     * Return a specific user.
     *
     * @param name
     *   The user to get.
     * @param checkWithoutProtocol
     *   Whether to seek based on the protocol or not.
     *
     * @return User
     */
    public User getUser(String name, boolean checkWithoutProtocol) {
        User user = null;
        List<User> users = this.getAllUsers();
        for (User u: users) {

            if (checkWithoutProtocol) {
                if (u.getMeWithoutProtocol().equals(name)) {
                    user = u;
                }
            }
            else {
                if (u.getMe().equals(name)) {
                    user = u;
                }
            }
        }

        return user;
    }

}