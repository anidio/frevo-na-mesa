import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

const ProtectedRoute = () => {
    const { isLoggedIn } = useAuth();

    if (!isLoggedIn) {
        // Se o usuário não estiver logado, redireciona para a página de login
        return <Navigate to="/login" replace />;
    }

    // Se o usuário estiver logado, renderiza o conteúdo da rota filha (a página protegida)
    return <Outlet />;
};

export default ProtectedRoute;