package com.mobnetic.compose.sharedelement.sample

import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Box
import androidx.compose.foundation.Image
import androidx.compose.foundation.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.lazy.LazyColumnFor
import androidx.compose.material.ListItem
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.mobnetic.compose.sharedelement.SharedElement
import com.mobnetic.compose.sharedelement.SharedElementType
import com.mobnetic.compose.sharedelement.SharedElementsRoot

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SharedElementsRoot {
                    when (val selectedUser = viewModel.selectedUser.value) {
                        null -> UsersListScreen()
                        else -> UserDetailsScreen(selectedUser)
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        if (viewModel.selectedUser.value != null) {
            viewModel.selectedUser.value = null
        } else {
            super.onBackPressed()
        }
    }
}

class ViewModel(var selectedUser: MutableState<User?> = mutableStateOf(null))

val viewModel = ViewModel()

@Composable
fun UsersListScreen() {
    LazyColumnFor(users) { user ->
        ListItem(
            modifier = Modifier.clickable(onClick = { viewModel.selectedUser.value = user }),
            icon = {
                SharedElement(tag = user, type = SharedElementType.FROM) {
                    Image(
                        asset = vectorResource(id = user.avatar),
                        modifier = Modifier.preferredSize(48.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            },
            text = {
                SharedElement(tag = user to user.name, type = SharedElementType.FROM) {
                    Text(text = user.name)
                }
            }
        )
    }
}

@Composable
fun UserDetailsScreen(user: User) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Box(modifier = Modifier.preferredSize(200.dp)
            .gravity(Alignment.CenterHorizontally)
            .clickable(onClick = { viewModel.selectedUser.value = null })
        ) {
            SharedElement(tag = user, type = SharedElementType.TO) {
                Image(
                    asset = vectorResource(id = user.avatar),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
        SharedElement(
            tag = user to user.name,
            type = SharedElementType.TO,
            modifier = Modifier.gravity(Alignment.CenterHorizontally)
        ) {
            Text(text = user.name, style = MaterialTheme.typography.h1)
        }
    }
}

data class User(@DrawableRes val avatar: Int, val name: String)

val users = listOf(
    User(R.drawable.avatar_1, "Adam"),
    User(R.drawable.avatar_2, "Andrew"),
    User(R.drawable.avatar_3, "Anna"),
    User(R.drawable.avatar_4, "Boris"),
    User(R.drawable.avatar_5, "Carl"),
    User(R.drawable.avatar_6, "Donna"),
    User(R.drawable.avatar_7, "Emily"),
    User(R.drawable.avatar_8, "Fiona"),
    User(R.drawable.avatar_9, "Grace"),
    User(R.drawable.avatar_10, "Irene"),
    User(R.drawable.avatar_11, "Jack"),
    User(R.drawable.avatar_12, "Jake"),
    User(R.drawable.avatar_13, "Mary"),
    User(R.drawable.avatar_14, "Peter"),
    User(R.drawable.avatar_15, "Rose"),
    User(R.drawable.avatar_16, "Victor")
)
