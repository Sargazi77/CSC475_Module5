package com.example.photogalary

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.photogalary.ui.theme.PhotoGalaryTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ColorFilter

class MainActivity : ComponentActivity() {

    // Register for the result of a permission request
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                loadGallery() // Load the gallery if permission is granted
            } else {
                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("PhotoGallery", "App launched, checking permissions...") // Debug log
        checkAndRequestPermissions()
    }

    // Check and request permissions based on Android version
    private fun checkAndRequestPermissions() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(permission)
        } else {
            loadGallery() // Permission already granted
        }
    }

    // Load the gallery in the background using a coroutine
    private fun loadGallery() {
        CoroutineScope(Dispatchers.Main).launch {
            val images = withContext(Dispatchers.IO) {
                fetchImages() // Fetch images on a background thread
            }
            displayGallery(images) // Display images on the main thread
        }
    }

    // Fetch images from the device's MediaStore
    private fun fetchImages(): List<String> {
        val imageList = mutableListOf<String>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media.DATA)

        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            while (cursor.moveToNext()) {
                val imagePath = cursor.getString(columnIndex)
                Log.d("PhotoGallery", "Image found: $imagePath") // Debug log
                imageList.add(imagePath)
            }
        }

        Log.d("PhotoGallery", "Total images fetched: ${imageList.size}") // Debug log
        return imageList
    }

    // Display the images using Jetpack Compose's LazyVerticalGrid
    private fun displayGallery(imageList: List<String>) {
        setContent {
            PhotoGalaryTheme {
                imageGallery(imageList = imageList) // Use lowercase function name
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun imageGallery(imageList: List<String>) {
    // State to manage the current image index
    var selectedImageIndex by remember { mutableStateOf<Int?>(null) }

    // If an image is selected, show it in a full-screen dialog with Next/Back buttons
    selectedImageIndex?.let { index ->
        AlertDialog(
            onDismissRequest = { selectedImageIndex = null },
            title = { Text(text = "Image ${index + 1} of ${imageList.size}") },
            text = {
                AsyncImage(
                    model = imageList[index],
                    contentDescription = "Image $index",
                    modifier = Modifier.padding(8.dp)
                )
            },
            confirmButton = {
                Row {
                    // Back Button: Disabled on the first image
                    Button(
                        onClick = { selectedImageIndex = (selectedImageIndex ?: 0) - 1 },
                        enabled = index > 0  // Disable if on the first image
                    ) {
                        Text("Back")
                    }

                    Spacer(modifier = Modifier.padding(8.dp))  // Space between buttons

                    // Next Button: Disabled on the last image
                    Button(
                        onClick = { selectedImageIndex = (selectedImageIndex ?: 0) + 1 },
                        enabled = index < imageList.size - 1  // Disable if on the last image
                    ) {
                        Text("Next")
                    }
                }
            },
            dismissButton = {
                Button(onClick = { selectedImageIndex = null }) {
                    Text("Close")
                }
            }
        )
    }

    // LazyVerticalGrid to display images
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.padding(8.dp)
    ) {
        items(imageList.size) { index ->
            AsyncImage(
                model = imageList[index],
                contentDescription = "Image $index",
                modifier = Modifier
                    .padding(4.dp)
                    .clickable { selectedImageIndex = index }  // Open dialog with selected image
            )
        }
    }
}