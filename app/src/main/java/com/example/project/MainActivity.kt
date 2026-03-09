package com.example.project

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.project.ui.theme.ProjectTheme
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.project.roomDb.User
import com.example.project.roomDb.UserDatabase
import com.example.project.viewModel.UserViewModel
import com.example.project.viewModel.Repository
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.activity.compose.setContent
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

class AppStuff(
    var viewModel: UserViewModel,
    var lifecycleOwner: LifecycleOwner,
)


class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {

        val db by lazy {
            Room.databaseBuilder(
                applicationContext,
                UserDatabase::class.java,
                name = "user.db"
            ).build()
        }

        val viewModel by viewModels<UserViewModel>(
            factoryProducer = {
                object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return UserViewModel(Repository(db)) as T
                    }
                }
            }
        )

        val appStuff: AppStuff = AppStuff(
            viewModel = viewModel,
            lifecycleOwner = this,
        )

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProjectTheme {
                Navigation(appStuff, applicationContext)
            }
        }
    }
}

const val mainScreenRoute = "mainScreen"
const val messageScreenRoute = "messageScreen"
const val settingsScreenRoute = "settingsScreen"
const val cameraScreenRoute = "cameraScreen"


@Composable
fun Navigation(appStuff: AppStuff, appContext: Context) {
    var imageUriState = remember {
        mutableStateOf<Uri?>(null)
    }
    var usernameState = remember {
        mutableStateOf<String>("")
    }
    var userList by remember {
        mutableStateOf(listOf<User>())
    }
    appStuff.viewModel.getUsers().observe(appStuff.lifecycleOwner) {
        userList = it
    }
    if (userList.isNotEmpty()) {
        imageUriState.value = userList[0].image.toUri()
        usernameState.value = userList[0].name
    }

    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = mainScreenRoute) {
        composable(route = mainScreenRoute) {
            MainScreen(navController = navController)
        }
        composable(route = messageScreenRoute) {
            MessageScreen(
                navController = navController, imageUriState.value,
                usernameState.value
            )
        }
        composable(route = settingsScreenRoute) {
            SettingsScreen(
                navController = navController,
                appStuff.viewModel,
                imageUriState,
                usernameState
            )
        }
        composable(route = cameraScreenRoute) {
            CameraScreen(appStuff, appContext)
        }
    }


}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    appStuff: AppStuff,
    appContext: Context
) {
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState()
    val controller = remember {
        LifecycleCameraController(appContext).apply {
            setEnabledUseCases(
                CameraController.IMAGE_CAPTURE or
                        CameraController.VIDEO_CAPTURE
            )
        }
    }
    val viewModel = viewModel<MainViewModel>()
    val bitmaps by viewModel.bitmaps.collectAsState()

    fun takePhoto(
        controller: LifecycleCameraController,
        onPhotoTaken: (Bitmap) -> Unit
    ) {
        controller.takePicture(
            ContextCompat.getMainExecutor(appContext),
            object : OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)

                    val matrix = Matrix().apply {
                        postRotate(image.imageInfo.rotationDegrees.toFloat())
                    }
                    val rotatedBitmap = Bitmap.createBitmap(
                        image.toBitmap(),
                        0,
                        0,
                        image.width,
                        image.height,
                        matrix,
                        true
                    )

                    onPhotoTaken(rotatedBitmap)
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    Log.e("Camera", "Couldn't take photo: ", exception)
                }
            }
        )
    }

    fun hasRequiredPermissions(): Boolean {
        var cameraPermission =
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.CAMERA
            )
        var audioPermission =
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.RECORD_AUDIO
            )
        return (cameraPermission == PackageManager.PERMISSION_GRANTED &&
                audioPermission == PackageManager.PERMISSION_GRANTED)
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp,
        sheetContent = {
            PhotoBottomSheetContent(
                bitmaps = bitmaps,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            CameraPreview(
                controller = controller,
                modifier = Modifier
                    .fillMaxSize()
            )

            IconButton(
                onClick = {
                    controller.cameraSelector =
                        if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else CameraSelector.DEFAULT_BACK_CAMERA
                },
                modifier = Modifier
                    .offset(16.dp, 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Switch camera"
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                IconButton(
                    onClick = {
                        scope.launch {
                            scaffoldState.bottomSheetState.expand()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Photo,
                        contentDescription = "Open gallery"
                    )
                }
                IconButton(
                    onClick = {
                        takePhoto(
                            controller = controller,
                            onPhotoTaken = viewModel::onTakePhoto
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Take photo"
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(navController: NavController) {

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,

        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 50.dp)
    ) {
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            onClick = {
                navController.navigate(messageScreenRoute)
            },
        ) {
            Text(text = "Strange Man")
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                navController.navigate(settingsScreenRoute)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
        ) {
            Text(text = "Settings")
        }
    }
}


@Composable
fun MessageScreen(
    navController: NavController, uri: Uri?,
    username: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                modifier = Modifier.width(190.dp),
                onClick = {
                    navController.popBackStack()
                },
            ) {
                Text(text = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                modifier = Modifier.width(190.dp),
                onClick = {
                    navController.navigate(settingsScreenRoute)
                },
                colors = ButtonDefaults.buttonColors(Color.DarkGray)
            ) {
                Text(text = "Settings")
            }
        }
        ProjectTheme {
            Conversation(SampleData.conversationSample, uri, username)
        }
    }
}

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: UserViewModel,
    imageUriState: MutableState<Uri?>,
    usernameState: MutableState<String>
) {
    val context = LocalContext.current

    val singleImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            imageUriState.value = uri

            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 50.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(onClick = {
            val user: User = User(usernameState.value, imageUriState.value.toString(), 0)
            viewModel.upsertUser(user)
        }) {
            Text(text = "set data")
        }

        Column(modifier = Modifier.clickable(onClick = {
            singleImagePickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        })) {
            AsyncProfilePicture(imageUriState.value)
        }

        TextField(
            value = usernameState.value, onValueChange = { newText: String ->
                usernameState.value = newText
            },
            label = { Text(text = "Username") })
        Button(
            onClick = {
                navController.popBackStack()
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(),
        ) {
            Text(text = "Back")
        }

    }
}

data class Message(val author: String, val body: String)

@Composable
fun AsyncProfilePicture(uri: Uri?) {
    AsyncImage(
        model = uri,
        contentDescription = "",
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .border(1.5.dp, MaterialTheme.colorScheme.secondary, CircleShape)
    )
}

@Composable
fun ProfilePicture(picture: Int) {
    Image(
        painter = painterResource(picture),
        contentDescription = null,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .border(1.5.dp, MaterialTheme.colorScheme.secondary, CircleShape)
    )
}

@Composable
fun MessageCard(msg: Message, uri: Uri?, username: String) {
    Row(modifier = Modifier.padding(all = 8.dp)) {
        AsyncProfilePicture(uri)
        Spacer(modifier = Modifier.width(8.dp))

        // We keep track if the message is expanded or not in this
        // variable
        var isExpanded by remember { mutableStateOf(false) }
        // surfaceColor will be updated gradually from one color to the other
        val surfaceColor by animateColorAsState(
            if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        )

        // We toggle the isExpanded variable when we click on this Column
        Column(modifier = Modifier.clickable { isExpanded = !isExpanded }) {
            var displayName = msg.author
            if (username != "") {
                displayName = username
            }
            Text(
                text = displayName,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 1.dp,
                // surfaceColor color will be changing gradually from primary to surface
                color = surfaceColor,
                // animateContentSize will change the Surface size gradually
                modifier = Modifier
                    .animateContentSize()
                    .padding(1.dp)
            ) {
                Text(
                    text = msg.body,
                    modifier = Modifier.padding(all = 4.dp),
                    // If the message is expanded, we display all its content
                    // otherwise we only display the first line
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun Conversation(messages: List<Message>, uri: Uri?, username: String) {
    LazyColumn {
        items(messages) { message ->
            MessageCard(message, uri, username)
        }
    }
}