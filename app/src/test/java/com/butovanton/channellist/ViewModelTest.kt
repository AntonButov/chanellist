package com.butovanton.channellist

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.butovanton.channellist.data.IFavoriteRepository
import com.butovanton.channellist.domain.Channel
import com.butovanton.channellist.domain.IRepository
import com.butovanton.channellist.presentation.TabsViewModel
import com.butovanton.channellist.presentation.components.TabScreen
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ViewModelTest {

    @Test
    fun `on init have empty list of channels`() {
        val viewModel = TabsViewModel(
            mockk<IRepository>(relaxed = true),
            mockk<IFavoriteRepository>(relaxed = true),
            savedStateHandle = SavedStateHandle())
        assertEquals(viewModel.searchQuery.value, "")
    }

    @Test
    fun `on init have started tab`() {
        val viewModel = TabsViewModel(
            mockk<IRepository>(relaxed = true),
            mockk<IFavoriteRepository>(relaxed = true),
            savedStateHandle = SavedStateHandle()
        )
        assertEquals(viewModel.tab.value, TabScreen.All)
    }

    @Test
    fun `on init should return some list`() = runBlocking {
        val repository = mockk<IRepository>()
        coEvery { repository.getChannels() } returns flowOf(listOf(Channel("name", null, null)))
        val favoriteRepository = mockk<IFavoriteRepository>()
        coEvery { favoriteRepository.getFavorites() } returns flowOf(listOf("name"))
        val viewModel = TabsViewModel(
            repository,
            favoriteRepository,
            savedStateHandle = SavedStateHandle()
        )
        val result = viewModel.channels.first()
        assertEquals(result.first().name, "name")
        assertEquals(result.count(), 1)
    }

    @Test
    fun `on search 'st' should return the chanel`() = runBlocking {
        val repository = mockk<IRepository>()
        coEvery { repository.getChannels() } returns flowOf(
            listOf(
                Channel("smart", null, null),
                Channel("star", null, null)
            )
        )
        val favoriteRepository = mockk<IFavoriteRepository>()
        coEvery { favoriteRepository.getFavorites() } returns flowOf(listOf("smart", "star"))
        val viewModel = TabsViewModel(
            repository,
            favoriteRepository,
            savedStateHandle = SavedStateHandle()
        )
        viewModel.channels.test {
            viewModel.onSearchQueryChanged("st")
            assertEquals(viewModel.searchQuery.value, "st")
            val result = awaitItem()
            assertEquals(result.first().name, "star")
            assertEquals(result.count(), 1)
            viewModel.onSearchQueryChanged("")
            val resultSecond = awaitItem()
            assertEquals(resultSecond.count(),2)
            verify(exactly = 1) { repository.getChannels() }
        }
    }

    @Test
    fun `on tab select should change channels`() = runBlocking {
        val repository = mockk<IRepository>()
        coEvery { repository.getChannels() } returns flowOf(
            listOf(
                Channel("first", null, null),
                Channel("favorite", null, null)
            )
        )
        val favoriteRepository = mockk<IFavoriteRepository>()
        every { favoriteRepository.getFavorites() } returns flowOf(
            listOf("favorite")
        )
        val viewModel = TabsViewModel(
            repository,
            favoriteRepository,
            savedStateHandle = SavedStateHandle()
        )
        viewModel.channels.test {
            viewModel.onTabSelect(TabScreen.Favorite)
            assertEquals(viewModel.tab.value, TabScreen.Favorite)
            val result = awaitItem()
            assertEquals(result.first().name, "favorite")
            assertEquals(result.count(), 1)
            viewModel.onTabSelect(TabScreen.All)
            assertEquals(viewModel.tab.value, TabScreen.All)
            val resultSecond = awaitItem()
            assertEquals(resultSecond.count(), 2)
            verify(exactly = 1) { repository.getChannels() }
        }
    }
}