package me.lazmaid.fireredux

import com.google.firebase.database.DatabaseError
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isBlank
import com.nhaarman.mockito_kotlin.argumentCaptor
import me.lazmaid.fireredux.extension.FirebaseException
import me.lazmaid.fireredux.model.Note
import me.lazmaid.fireredux.navigation.DetailViewKey
import me.lazmaid.fireredux.navigation.ViewNavigator
import me.lazmaid.fireredux.presentation.HomeViewModelStore
import me.lazmaid.fireredux.presentation.HomeViewModelStore.Action
import me.lazmaid.fireredux.repository.NoteRepository
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.*
import rx.Observable
import rx.observers.TestSubscriber

/**
 * Created by VerachadW on 12/31/2016 AD.
 */

@RunWith(JUnitPlatform::class)
class HomeViewModelSpec : Spek({
    describe("HomeViewModelStore class") {
        val mockRepository = mock(NoteRepository::class.java)
        val mockNavigator = mock(ViewNavigator::class.java)
        var viewModel = HomeViewModelStore(mockNavigator, mockRepository)
        val state = HomeViewModelStore.State()
        beforeEachTest {
            viewModel = HomeViewModelStore(mockNavigator, mockRepository)
        }
        describe("Reducer") {
            given("GetNotesAction is dispatched") {
                it("should not mutate the state") {
                    val newState = viewModel.reducer.reduce(state, Action.GetNotesAction())
                    assertThat(newState, equalTo(state))
                }
            }
            given("ShowNotesAction is dispatched") {
                it("should mutate the state with new note list and clear error message") {
                    val data = listOf(Note(title = "Note#1"), Note(title = "Note#2"))
                    val newState = viewModel.reducer.reduce(state, Action.ShowNotesAction(data))
                    assertThat(newState.items, equalTo(data))
                    assertThat(newState.errorMessage, isBlank)
                }
            }
            given("ShowErrorAction is dispatched") {
                it("should mutate the state with error message") {
                    val newState = viewModel.reducer.reduce(state, Action.ShowErrorAction("<error>"))
                    assertThat(newState.errorMessage, equalTo("<error>"))
                }
            }
        }
        describe("GetNotes Epic") {
            on("Success") {
                val data = listOf(Note(title = "Note#1"), Note(title = "Note#2"))
                it("should map to ShowNotesAction") {
                    `when`(mockRepository.getNotes()).thenReturn(Observable.just(data))
                    val subscriber = TestSubscriber<Any>()
                    val actionObservable = viewModel.getNotesEpic.map(Observable.just(Action.GetNotesAction()), viewModel.store)
                    actionObservable.subscribe(subscriber)
                    val result = subscriber.onNextEvents[0]
                    assertThat(result is Action.ShowNotesAction, equalTo(true))
                    assertThat( (result as Action.ShowNotesAction).notes, equalTo(data))
                }
            }
            on("Failure") {
                val error = FirebaseException(DatabaseError.UNKNOWN_ERROR, "test")
                it("should map to ShowErrorAction") {
                    `when`(mockRepository.getNotes()).thenReturn(Observable.error(error))
                    val subscriber = TestSubscriber<Any>()
                    val actionObservable = viewModel.getNotesEpic.map(Observable.just(Action.GetNotesAction()), viewModel.store)
                    actionObservable.subscribe(subscriber)
                    val result = subscriber.onNextEvents[0]
                    assertThat(result is Action.ShowErrorAction, equalTo(true))
                    assertThat( (result as Action.ShowErrorAction).message, equalTo(error.message))
                }
            }
        }
        describe("Navigation") {
            describe("navigate to Note Detail") {
                it("should navigate to NoteDetail View with selected note id") {
                    val fakeNote = Note(title = "<title>")
                    viewModel.dispatch(Action.OpenNoteDetailAction(fakeNote))
                    argumentCaptor<DetailViewKey>().apply {
                        verify(mockNavigator, Mockito.times(1)).navigateTo(capture())
                        assertThat(firstValue.selectedNote, equalTo(fakeNote))
                    }
                }
            }
        }
    }
})



