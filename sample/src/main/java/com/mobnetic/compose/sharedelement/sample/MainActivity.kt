package com.mobnetic.compose.sharedelement.sample

import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.Composable
import androidx.compose.Model
import androidx.ui.core.Modifier
import androidx.ui.core.setContent
import androidx.ui.foundation.AdapterList
import androidx.ui.foundation.Clickable
import androidx.ui.foundation.Image
import androidx.ui.foundation.Text
import androidx.ui.graphics.ScaleFit
import androidx.ui.layout.Arrangement
import androidx.ui.layout.Column
import androidx.ui.layout.ColumnAlign
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.preferredSize
import androidx.ui.material.ListItem
import androidx.ui.material.MaterialTheme
import androidx.ui.res.vectorResource
import androidx.ui.unit.dp
import com.mobnetic.compose.sharedelement.SharedElement
import com.mobnetic.compose.sharedelement.SharedElementType
import com.mobnetic.compose.sharedelement.SharedElementsRoot

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SharedElementsRoot {
                    when (val selectedUser = viewModel.selectedUser) {
                        null -> UsersListScreen()
                        else -> UserDetailsScreen(selectedUser)
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        if (viewModel.selectedUser != null) {
            viewModel.selectedUser = null
        } else {
            super.onBackPressed()
        }
    }
}

@Model
class ViewModel(var selectedUser: User? = null)

val viewModel = ViewModel()

@Composable
fun UsersListScreen() {
    AdapterList(data = users) { user ->
        ListItem(
            icon = {
                SharedElement(tag = user, type = SharedElementType.FROM) {
                    Image(
                        asset = vectorResource(id = user.avatar),
                        modifier = Modifier.preferredSize(48.dp),
                        scaleFit = ScaleFit.FillMaxDimension
                    )
                }
            },
            text = {
                SharedElement(tag = user to user.name, type = SharedElementType.FROM) {
                    Text(text = user.name)
                }
            },
            onClick = { viewModel.selectedUser = user }
        )
    }
}

@Composable
fun UserDetailsScreen(user: User) {
    Column(modifier = Modifier.fillMaxSize(), arrangement = Arrangement.Center) {
        Clickable(
            onClick = { viewModel.selectedUser = null },
            modifier = Modifier.preferredSize(200.dp).gravity(ColumnAlign.Center)
        ) {
            SharedElement(tag = user, type = SharedElementType.TO) {
                Image(
                    asset = vectorResource(id = user.avatar),
                    modifier = Modifier.fillMaxSize(),
                    scaleFit = ScaleFit.FillMaxDimension
                )
            }
        }
        SharedElement(
            tag = user to user.name,
            type = SharedElementType.TO,
            modifier = Modifier.gravity(ColumnAlign.Center)
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
