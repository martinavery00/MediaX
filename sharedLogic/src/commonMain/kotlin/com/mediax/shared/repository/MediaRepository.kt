package com.mediax.shared.repository

import com.mediax.shared.model.DashboardSnapshot

interface MediaRepository {
    @Throws(Exception::class)
    suspend fun loadDashboard(): DashboardSnapshot
}
