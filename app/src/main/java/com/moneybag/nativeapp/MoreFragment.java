package com.moneybag.nativeapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.moneybag.nativeapp.data.CsvManager;
import com.moneybag.nativeapp.data.MoneyBagRepository;
import androidx.appcompat.app.AlertDialog;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;
import com.moneybag.nativeapp.data.SyncManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.moneybag.nativeapp.databinding.FragmentMoreBinding;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MoreFragment extends Fragment {

    private FragmentMoreBinding binding;
    private CsvManager csvManager;
    private SyncManager syncManager;
    private SharedPreferences prefs;
    private ActivityResultLauncher<String> importLauncher;
    private ActivityResultLauncher<String> backupLauncher;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private MoneyBagRepository repository;
    private FirebaseAuth auth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private GoogleSignInClient googleSignInClient;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new MoneyBagRepository(requireActivity().getApplication());
        csvManager = new CsvManager(requireContext(), repository);
        syncManager = new SyncManager(requireContext(), repository);
        prefs = requireContext().getSharedPreferences("MoneyBagPrefs", Context.MODE_PRIVATE);
        auth = FirebaseAuth.getInstance();
        authStateListener = firebaseAuth -> {
            if (binding != null) updateLoginState();
        };

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()   // FIX: explicitly request profile (photo URL)
                .build();
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

        importLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) importCsv(uri);
        });

        backupLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument("text/csv"), uri -> {
            if (uri != null) exportCsv(uri);
        });

        googleSignInLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    firebaseAuthWithGoogle(account.getIdToken());
                } catch (ApiException e) {
                    Toast.makeText(getContext(), "Sign in failed: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        updateLoginState();
                        performSync();
                    } else {
                        Toast.makeText(getContext(), "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMoreBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUI();
        updateLoginState();
        updateSyncStatus();
    }

    private void setupUI() {
        // Profile card tapped → sign in if not signed in
        binding.cardProfile.setOnClickListener(v -> {
            if (auth.getCurrentUser() == null) {
                // FIX: silent sign-in first, then launch intent only if needed
                googleSignInClient.silentSignIn().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        firebaseAuthWithGoogle(task.getResult().getIdToken());
                    } else {
                        googleSignInLauncher.launch(googleSignInClient.getSignInIntent());
                    }
                });
            }
        });

        binding.btnUnlinkProfile.setOnClickListener(v ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("Sign Out")
                        .setMessage("Sign out from Google Cloud Sync?")
                        .setPositiveButton("Sign Out", (d, w) -> {
                            auth.signOut();
                            googleSignInClient.signOut();
                            updateLoginState();
                            updateSyncStatus();
                        })
                        .setNegativeButton("Cancel", null)
                        .show());

        binding.btnSyncNow.setOnClickListener(v -> performSync());
        binding.btnSyncFreqProfile.setOnClickListener(v -> openFragment(new SyncFrequencyFragment()));

        setupSettingsItem(binding.menuCategories.getRoot(), "Categories", "Manage icons and labels",
                R.drawable.ic_categories_modern, () -> openFragment(new CategoriesFragment()));

        setupSettingsItem(binding.menuCurrency.getRoot(), "Currency", "Set default currency",
                R.drawable.ic_currency, () -> openFragment(new CurrencySettingFragment()));

        setupSettingsItem(binding.menuSecurity.getRoot(), "Security", "Passcode and privacy",
                R.drawable.ic_shield, () -> {
                    Intent intent = new Intent(getContext(), PasscodeActivity.class);
                    intent.putExtra("mode", "setup");
                    startActivity(intent);
                });

        boolean realTimeSync = prefs.getBoolean("real_time_sync", true);
        setupSettingsSwitch(binding.menuRealTimeSync.getRoot(), "Real-time Sync", "Sync changes immediately",
                R.drawable.ic_sync_modern, realTimeSync,
                checked -> prefs.edit().putBoolean("real_time_sync", checked).apply());

        setupSettingsItem(binding.menuBackup.getRoot(), "Export CSV", "Backup to local storage",
                R.drawable.ic_cloud_upload,
                () -> backupLauncher.launch("moneybag_backup_" + System.currentTimeMillis() + ".csv"));

        setupSettingsItem(binding.menuImport.getRoot(), "Import CSV", "Restore data from file",
                R.drawable.ic_cloud_download, () -> importLauncher.launch("text/csv"));

        setupSettingsItem(binding.menuPcAccess.getRoot(), "PC Access", "View data in browser",
                R.drawable.ic_laptop, () -> openFragment(new PcAccessFragment()));

        setupSettingsItem(binding.menuDeleteData.getRoot(), "Delete All Data", "Reset application",
                R.drawable.ic_delete_forever, () -> openFragment(new DeleteDataFragment()));

        // Make Delete red
        com.moneybag.nativeapp.databinding.LayoutSettingsItemBinding deleteB =
                com.moneybag.nativeapp.databinding.LayoutSettingsItemBinding.bind(binding.menuDeleteData.getRoot());
        deleteB.title.setTextColor(requireContext().getColor(android.R.color.holo_red_dark));
        deleteB.icon.setImageTintList(android.content.res.ColorStateList.valueOf(
                requireContext().getColor(android.R.color.holo_red_dark)));

        setupSettingsItem(binding.menuShare.getRoot(), "Share App", "Tell friends about MoneyBag",
                android.R.drawable.ic_menu_share, () -> {
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("text/plain");
                    i.putExtra(Intent.EXTRA_TEXT, "Check out MoneyBag — simple personal finance!");
                    startActivity(Intent.createChooser(i, "Share via"));
                });

        setupSettingsItem(binding.menuRate.getRoot(), "Rate Us", "Leave a review",
                android.R.drawable.btn_star_big_on, () -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=" + requireContext().getPackageName())));
                    } catch (Exception e) {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=" +
                                        requireContext().getPackageName())));
                    }
                });

        setupSettingsItem(binding.menuTerms.getRoot(), "Privacy Policy", "Read our policy",
                android.R.drawable.ic_menu_info_details,
                () -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/terms"))));

        setupSettingsItem(binding.menuAppInfo.getRoot(), "App Info", "Version 1.0.0",
                android.R.drawable.ic_menu_help,
                () -> Toast.makeText(getContext(), "MoneyBag v1.0.0", Toast.LENGTH_SHORT).show());
    }

    private void setupSettingsItem(View view, String title, String subtitle, int iconRes, Runnable onClick) {
        com.moneybag.nativeapp.databinding.LayoutSettingsItemBinding b =
                com.moneybag.nativeapp.databinding.LayoutSettingsItemBinding.bind(view);
        b.title.setText(title);
        b.subtitle.setText(subtitle);
        b.subtitle.setVisibility(View.VISIBLE);
        b.icon.setImageResource(iconRes);
        view.setOnClickListener(v -> onClick.run());
    }

    private void setupSettingsSwitch(View view, String title, String subtitle, int iconRes,
                                     boolean initial, OnCheckedChangeListener listener) {
        com.moneybag.nativeapp.databinding.LayoutSettingsSwitchBinding b =
                com.moneybag.nativeapp.databinding.LayoutSettingsSwitchBinding.bind(view);
        b.title.setText(title);
        b.subtitle.setText(subtitle);
        b.subtitle.setVisibility(View.VISIBLE);
        b.icon.setImageResource(iconRes);
        b.settingsSwitch.setChecked(initial);
        b.settingsSwitch.setOnCheckedChangeListener((btn, checked) -> listener.onChecked(checked));
    }

    private void openFragment(Fragment fragment) {
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }

    /** FIX: updateLoginState loads profile photo properly using Glide with HTTP headers bypass */
    private void updateLoginState() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            binding.textProfileName.setText(user.getDisplayName() != null ? user.getDisplayName() : "Google User");
            binding.textProfileEmail.setText(user.getEmail() != null ? user.getEmail() : "");
            binding.btnUnlinkProfile.setVisibility(View.VISIBLE);
            binding.cardSyncStatus.setVisibility(View.VISIBLE);
            binding.btnSyncFreqProfile.setVisibility(View.VISIBLE);

            // FIX: reload user to get fresh photo URL, then load with Glide
            user.reload().addOnCompleteListener(task -> {
                FirebaseUser refreshed = auth.getCurrentUser();
                if (refreshed == null || getContext() == null) return;
                Uri photoUri = refreshed.getPhotoUrl();
                if (photoUri != null) {
                    // Append sz=200 to force higher-res Google profile photo
                    String photoUrl = photoUri.toString();
                    if (photoUrl.contains("googleusercontent.com") && !photoUrl.contains("sz=")) {
                        photoUrl += "?sz=200";
                    }
                    Glide.with(this)
                            .load(photoUrl)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.ic_nav_accounts)
                            .error(R.drawable.ic_nav_accounts)
                            .circleCrop()
                            .into(binding.imageProfile);
                } else {
                    binding.imageProfile.setImageResource(R.drawable.ic_nav_accounts);
                }
            });
        } else {
            binding.textProfileName.setText("Sign in with Google");
            binding.textProfileEmail.setText("Backup and sync your data");
            binding.btnUnlinkProfile.setVisibility(View.GONE);
            binding.cardSyncStatus.setVisibility(View.GONE);
            binding.btnSyncFreqProfile.setVisibility(View.GONE);
            binding.imageProfile.setImageResource(R.drawable.ic_nav_accounts);
        }
    }

    private void updateSyncStatus() {
        if (auth.getCurrentUser() == null) {
            binding.cardSyncStatus.setVisibility(View.GONE);
            return;
        }
        binding.cardSyncStatus.setVisibility(View.VISIBLE);
        long lastSync = prefs.getLong("last_sync_timestamp", 0);
        if (lastSync == 0) {
            binding.textLastSync.setText("Never synced");
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
            binding.textLastSync.setText("Last synced: " + sdf.format(new Date(lastSync)));
        }
        binding.textSyncStatus.setText("Cloud Connected");
    }

    private void performSync() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Sign in to sync", Toast.LENGTH_SHORT).show();
            return;
        }
        binding.btnSyncNow.setEnabled(false);
        binding.textSyncStatus.setText("Syncing…");
        binding.iconSyncStatus.animate().rotationBy(360).setDuration(800).start();

        syncManager.sync(new SyncManager.SyncCallback() {
            @Override public void onSuccess() {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    binding.btnSyncNow.setEnabled(true);
                    updateSyncStatus();
                    Toast.makeText(getContext(), "Sync complete", Toast.LENGTH_SHORT).show();
                });
            }
            @Override public void onError(String message) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    binding.btnSyncNow.setEnabled(true);
                    binding.textSyncStatus.setText("Sync failed");
                    Toast.makeText(getContext(), "Sync error: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void importCsv(Uri uri) {
        csvManager.importCsv(uri, new CsvManager.ImportCallback() {
            @Override public void onSuccess() {
                if (getActivity() != null)
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Import successful", Toast.LENGTH_SHORT).show());
            }
            @Override public void onError(String message) {
                if (getActivity() != null)
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void exportCsv(Uri uri) {
        csvManager.exportCsv(uri, new CsvManager.ExportCallback() {
            @Override public void onSuccess() {
                if (getActivity() != null)
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Backup saved", Toast.LENGTH_SHORT).show());
            }
            @Override public void onError(String message) {
                if (getActivity() != null)
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_LONG).show());
            }
        });
    }

    @Override public void onStart() {
        super.onStart();
        if (auth != null && authStateListener != null) auth.addAuthStateListener(authStateListener);
    }
    @Override public void onStop() {
        super.onStop();
        if (auth != null && authStateListener != null) auth.removeAuthStateListener(authStateListener);
    }
    @Override public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private interface OnCheckedChangeListener { void onChecked(boolean checked); }
}
